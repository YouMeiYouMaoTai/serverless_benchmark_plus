use std::fs::File;

use crate::{parse_platform::Platform, parse_test_mode::TestMode, Metric};

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
