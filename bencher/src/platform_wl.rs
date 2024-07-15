use tokio::process;

use crate::PlatformOps;
use std::collections::HashSet;
use std::{collections::HashMap, fs::File, io::BufReader, str::from_utf8};

pub struct PlatfromWl {
    master_url: String,
    worker_url: String,
    gen_demos: HashSet<String>,
}

impl PlatfromWl {
    pub fn new() -> Self {
        let file = File::open("../middlewares/cluster_config.yml").unwrap();
        let reader = BufReader::new(file);
        let config: HashMap<String, HashMap<String, serde_yaml::Value>> =
            serde_yaml::from_reader(reader).unwrap();
        let mut res = Self {
            master_url: "".to_owned(),
            worker_url: "".to_owned(),
            gen_demos: HashSet::new(),
        };
        for (_, conf) in config {
            if conf.contains_key("is_master") {
                res.master_url = format!(
                    "http://{}:2501",
                    conf.get("ip").unwrap().as_str().unwrap().to_owned()
                );
            } else {
                res.worker_url = format!(
                    "http://{}:2501",
                    conf.get("ip").unwrap().as_str().unwrap().to_owned()
                );
            }
        }
        res
    }
}

impl PlatformOps for PlatfromWl {
    async fn remove_all_fn(&self) {}
    async fn upload_fn(&mut self, demo: &str, rename_sub: &str) {
        if !self.gen_demos.contains(demo) {
            let res = process::Command::new("python3")
                .args(&["../demos/scripts/1.gen_waverless_app.py", demo])
                .status()
                .await
                .expect(&format!("Failed to gen demo {}", demo));
            assert!(res.success());
            self.gen_demos.insert(demo.to_owned());
        }
        process::Command::new("python3")
            .args(&["../middlewares/waverless/3.add_func.py", demo, rename_sub])
            .status()
            .await
            .expect(&format!("Failed to add func {} as {}", demo, rename_sub));
    }
    async fn call_fn(&self, app: &str, func: &str, arg_json_value: &serde_json::Value) -> String {
        println!("{}/{}/{}", self.master_url, app, func);
        let res = reqwest::Client::new()
            .post(format!("{}/{}/{}", self.master_url, app, func))
            .body(serde_json::to_string(&arg_json_value).unwrap())
            .send()
            .await
            .unwrap_or_else(|e| panic!("err: {:?}", e))
            .text()
            .await;
        res.unwrap()
    }
}
