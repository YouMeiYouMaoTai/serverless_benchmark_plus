use parking_lot::Mutex;
use rand::{Rng, SeedableRng};
use rand_chacha::ChaCha8Rng;
use std::sync::Arc;

use crate::{
    common_prepare::link_source_app_data, config::Config, mode_call_once, parse::Cli,
    platform::PlatformOpsBind, RANDOM_SEED,
};

pub async fn prepare(config: &Config, platform: &mut PlatformOpsBind, cli: &Cli) {
    // for each source fn, prepare first
    let source_fns = config
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

    for one_source_fn in source_fns {
        mode_call_once::prepare(platform, RANDOM_SEED.to_owned(), one_source_fn, &config).await;
    }
    for (source, app, func) in replica_fns {
        // link source fn prepare dir to replica fn prepare dir
        link_source_app_data(&source, &app, &func).await;
    }
}

pub async fn call_bench(platform: PlatformOpsBind, cli: Cli, config: Config) {
    // unimplemented!();
    const TASK_COUNT: usize = 1000;
    const SLEEP_MAX_MS: u64 = 2000;
    const EACH_TASK_REQ_COUNT: usize = 100;

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

    for i in 0..TASK_COUNT {
        let req_count = rng.lock().gen_range(0..EACH_TASK_REQ_COUNT);
        let rng = rng.clone();
        let fn_list = fn_list.clone();
        let platform = platform.clone();
        let cli = cli.clone();
        let config = config.clone();
        tokio::spawn(async move {
            for i in 0..req_count {
                let (sleep_ms, fn_idx) = {
                    let mut rng = rng.lock();
                    (
                        rng.gen_range(0..SLEEP_MAX_MS),
                        rng.gen_range(0..fn_list.len()),
                    )
                };

                mode_call_once::call(
                    fn_list[fn_idx].0.as_str(),
                    fn_list[fn_idx].1.as_str(),
                    &platform,
                    &cli,
                    &config,
                )
                .await;

                tokio::time::sleep(std::time::Duration::from_millis(sleep_ms)).await;
            }
        });
    }
}
