use reqwest::{header, Client};

use crate::metric::Metric;

pub(crate) async fn upload_fn_call_metric(fnname: &str, metric: &Metric) {
    let cmd = format!(
        r#"echo "total_request_latency{{fnname=\"{}\"}} {}
req_trans_time{{fnname=\"{}\"}} {}
app_verify_time{{fnname=\"{}\"}} {}
cold_start_time{{fnname=\"{}\"}} {}
cold_start_time2{{fnname=\"{}\"}} {}
exec_time{{fnname=\"{}\"}} {}" | curl --data-binary @- http://localhost:9091/metrics/job/serverless_benchmark_plus"#,
        fnname,
        metric.get_total_req(),
        fnname,
        metric.get_req_trans_time(),
        fnname,
        metric.get_app_verify_time(),
        fnname,
        metric.get_cold_start_time(),
        fnname,
        metric.get_cold_start_time2(),
        fnname,
        metric.get_exec_time()
    );
    tokio::process::Command::new("sh")
        .arg("-c")
        .arg(cmd)
        .output()
        .await
        .unwrap();
}
