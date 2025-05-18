use std::fs::File;

use serde::{Deserialize, Serialize};

use crate::{parse_platform::Platform, parse_test_mode::TestMode};

/// unit: ms
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Metric {
    pub start_call_time: u64,
    pub req_arrive_time: u64,
    pub bf_exec_time: u64,
    pub recover_begin_time: u64,
    pub fn_start_time: u64,
    pub fn_end_time: u64,
    pub receive_resp_time: u64,
}

impl Metric {
    pub fn debug_print(&self) {
        // // let mut time_interval = vec![];
        // let mut cur_time_step = 0;
        // fn get_time_step_value(metric: &Metric, time_step: u64) -> u64 {
        //     match time_step {
        //         0 => metric.start_call_time,
        //         1 => metric.req_arrive_time,
        //         2 => metric.bf_exec_time,
        //         3 => metric.recover_begin_time,
        //         4 => metric.fn_start_time,
        //         5 => metric.fn_end_time,
        //         6 => metric.receive_resp_time,
        //         _ => panic!("invalid time step"),
        //     }
        // }
        // fn get_time_step_name(time_step: u64) -> &'static str {
        //     match time_step {
        //         0 => "start_call_time",
        //         1 => "req_arrive_time",
        //         2 => "bf_exec_time",
        //         3 => "recover_begin_time",
        //         4 => "fn_start_time",
        //         5 => "fn_end_time",
        //         6 => "receive_resp_time",
        //         _ => panic!("invalid time step"),
        //     }
        // }
        // fn find_next_bigger_step(metric: &Metric, cur_time_step: u64) -> Option<u64> {
        //     for i in (cur_time_step + 1)..7 {
        //         if get_time_step_value(metric, i) > get_time_step_value(metric, cur_time_step) {
        //             return Some(i);
        //         }
        //     }
        //     None
        // }
        // let mut debug_str = String::new();
        // while cur_time_step < 6 {
        //     let Some(next_time_step) = find_next_bigger_step(self, cur_time_step) else {
        //         break;
        //     };
        //     // time_interval.push(
        //     //     get_time_step_value(self, next_time_step)
        //     //         - get_time_step_value(self, cur_time_step),
        //     // );
        //     debug_str.push_str(&format!(
        //         "{}:{}->{}:{}: {}ms, \n",
        //         get_time_step_name(cur_time_step),
        //         get_time_step_value(self, cur_time_step),
        //         get_time_step_name(next_time_step),
        //         get_time_step_value(self, next_time_step),
        //         get_time_step_value(self, next_time_step)
        //             - get_time_step_value(self, cur_time_step)
        //     ));

        //     cur_time_step = next_time_step;
        // }
        tracing::debug!("metric raw: {:?}", self);
        // tracing::debug!("metric analysis: {}", debug_str);
        // let cold_start_latency = if self.req_arrive_time >= self.recover_begin_time {
        //     // no cold start
        //     0
        // } else {
        //     // tracing::debug!("cold start");
        //     self.recover_begin_time - self.req_arrive_time
        // };

        let exec_latency = self.fn_end_time - self.fn_start_time;
        let total_req_latency = self.receive_resp_time - self.start_call_time;
        let cold_trans_latency = total_req_latency - exec_latency;
        // let trans_latency = total_req_latency - exec_latency - cold_start_latency;

        tracing::debug!("cold + transfer latency: {} ms", cold_trans_latency);
        tracing::debug!("exec latency:            {} ms", exec_latency);
        // tracing::debug!("trans latency:         {} ms", trans_latency);
        tracing::debug!("total request latency:   {} ms", total_req_latency);
    }
    // println!(
    //     "\ntotal request latency: {}",
    //     receive_resp_time - start_call_ms
    // );

    // println!("- req trans time: {}", req_arrive_time - start_call_ms);
    // println!("- app verify time: {}", bf_exec_time - req_arrive_time);
    // println!("- cold start time: {}", recover_begin_time - bf_exec_time);
    // println!("- cold start time2: {}", fn_start_ms - recover_begin_time);
    // println!("- exec time:{}", fn_end_ms - fn_start_ms);

    pub fn get_total_req(&self) -> u64 {
        self.receive_resp_time - self.start_call_time
    }
    pub fn get_req_trans_time(&self) -> u64 {
        self.req_arrive_time - self.start_call_time
    }
    pub fn get_app_verify_time(&self) -> u64 {
        self.bf_exec_time - self.req_arrive_time
    }
    pub fn get_cold_start_time(&self) -> u64 {
        self.recover_begin_time - self.bf_exec_time
    }
    pub fn get_cold_start_time2(&self) -> u64 {
        self.fn_start_time - self.recover_begin_time
    }
    pub fn get_exec_time(&self) -> u64 {
        self.fn_end_time - self.fn_start_time
    }
}

pub struct Recorder {
    func: String,
    metrics: Vec<Metric>,
    mode: TestMode,
    platform: Platform,
}

impl Recorder {
    pub fn new(func: String, mode: TestMode, platform: Platform) -> Self {
        Recorder {
            func,
            metrics: Vec::new(),
            mode,
            platform,
        }
    }

    pub fn record(&mut self, metric: Metric) {
        self.metrics.push(metric);
    }

    fn record_name(&self) -> String {
        format!(
            "{}.{}.{}",
            self.func,
            self.mode.to_string(),
            self.platform.to_string()
        )
    }

    pub fn persist(&self) {
        // # serialize self to a file
        // 1. folder records
        let _ = std::fs::create_dir_all("records");
        // 2. open file
        let f = File::create(format!("records/{}", self.record_name())).unwrap();
        // 3. write to file
        serde_json::to_writer(f, &self.metrics).unwrap();
    }
}
