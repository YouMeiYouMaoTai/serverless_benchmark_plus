use parking_lot::Mutex;
use rand::{Rng, SeedableRng};
use rand_chacha::ChaCha8Rng;
use std::sync::Arc;

use crate::{
    common_prepare::link_source_app_data,
    config::Config,
    mode_call_once,
    parse::Cli,
    platform::{PlatformOps, PlatformOpsBind},
    RANDOM_SEED,
};

pub async fn prepare(config: &Config, platform: &mut PlatformOpsBind, cli: &Cli) {
    // for each source fn, prepare first
    let source_app_fns = config
        .benchlist
        .iter()
        .filter(|one_bench_target| {
            let (app, func) = one_bench_target.split_once('/').unwrap();
            config.models.contains_key(app)
        })
        .collect::<Vec<&String>>();
    let replica_fns = config
        .benchlist
        .iter()
        .filter(|one_bench_target| {
            let (app, func) = one_bench_target.split_once('/').unwrap();
            config.replicas.contains_key(app)
        })
        .map(|v| {
            let (app, func) = v.split_once('/').unwrap();
            let source_app = config.replicas.get(app).unwrap().source.clone();
            (source_app, app.to_string(), func.to_string())
        })
        .collect::<Vec<(String, String, String)>>();

    for one_source_app_fn in source_app_fns {
        // mode_call_once::prepare(
        //     platform,
        //     RANDOM_SEED.to_owned(),
        //     one_source_app_fn.split("/").next().unwrap(),
        //     &config,
        // )
        // .await;
    }
    tracing::info!("prepare replica fns data {:?}", replica_fns);
    for (source, app, func) in replica_fns {
        // link source fn prepare dir to replica fn prepare dir
        link_source_app_data(&source, &app, &func).await;
        platform.upload_fn(source.as_str(), app.as_str()).await;
    }
}

pub async fn call_bench(platform: PlatformOpsBind, cli: Cli, config: Config) {
    // unimplemented!();
    const TASK_COUNT: usize = 20;
    const SLEEP_MIN_MS: u64 = 1000;
    const SLEEP_MAX_MS: u64 = 9000;
    const EACH_TASK_REQ_COUNT: usize = 10;

    let platform = Arc::new(platform);
    let cli = Arc::new(cli);
    let config = Arc::new(config);
    // prepare fn list [(app/fn,fn_details)]
    let mut fn_list = vec![];
    for one_bench_target in &config.benchlist {
        let (app, func) = one_bench_target.split_once('/').unwrap();
        fn_list.push((
            app.to_string(),
            func.to_string(),
            config.get_fn_details(app, func).expect(
                format!("bench target {:?} supposed to be exist", one_bench_target).as_str(),
            ),
        ))
    }
    let fn_list = Arc::new(fn_list);

    let mut seed_u8_32: [u8; 32] = [0; 32];
    for (i, b) in RANDOM_SEED.as_bytes().iter().enumerate() {
        seed_u8_32[i] = *b;
    }

    let mut rng = Arc::new(Mutex::new(ChaCha8Rng::from_seed(seed_u8_32)));
    let mut tasks = vec![];
    let begin_time = std::time::Instant::now();
    for i in 0..TASK_COUNT {
        let req_count = rng.lock().gen_range(0..EACH_TASK_REQ_COUNT);
        let rng = rng.clone();
        let fn_list = fn_list.clone();
        let platform = platform.clone();
        let cli = cli.clone();
        let config = config.clone();
        let task = tokio::spawn(async move {
            let mut metrics = vec![];
            for i in 0..req_count {
                let (sleep_ms, fn_idx) = {
                    let mut rng = rng.lock();
                    (
                        rng.gen_range(SLEEP_MIN_MS..SLEEP_MAX_MS),
                        rng.gen_range(0..fn_list.len()),
                    )
                };

                tokio::time::sleep(std::time::Duration::from_millis(sleep_ms)).await;
                let metric = tokio::time::timeout(
                    std::time::Duration::from_secs(30),
                    mode_call_once::call(
                        fn_list[fn_idx].0.as_str(),
                        fn_list[fn_idx].1.as_str(),
                        &platform,
                        &cli,
                        &config,
                    ),
                )
                .await;

                let metric = match metric {
                    Ok(metric) => metric,
                    Err(err) => {
                        tracing::warn!("call fn {:?} timeout, err: {:?}", fn_list[fn_idx], err);

                        continue;
                    }
                };

                metrics.push(metric);
            }
            metrics
        });
        tasks.push(task);
    }

    let mut total_count = 0;
    let mut total_req_time = 0;
    let mut total_exec_time = 0;
    let mut failed_count = 0;
    for task in tasks {
        let metrics = task.await;
        if metrics.is_err() {
            failed_count += 1;
            continue;
        }
        let metrics = metrics.unwrap();
        for metric in metrics {
            let Some(metric) = metric else {
                failed_count += 1;
                continue;
            };
            if metric.total_req_time() > 10000 {
                // tracing::warn!("req_arrive_time: {:?} ms", metric.req_arrive_time);
                failed_count += 1;
                continue;
            }
            total_count += 1;
            total_req_time += metric.total_req_time();
            total_exec_time += metric.total_exec_time();
        }
    }
    tracing::info!("total_count:              {}", total_count);
    tracing::info!("failed_count:             {}", failed_count);
    tracing::info!(
        "avg req time:             {} ms",
        total_req_time as f64 / (total_count) as f64
    );
    tracing::info!(
        "avg exec time:            {} ms",
        total_exec_time as f64 / (total_count) as f64
    );
    tracing::info!(
        "avg trans+coldstart time: {} ms",
        (total_req_time - total_exec_time) as f64 / (total_count) as f64
    );
    let elapsed_time = begin_time.elapsed();
    tracing::info!(
        "avg qps:                  {} req/s",
        (total_count) as f64 / elapsed_time.as_secs_f64()
    );
}
