use tokio::process;

use crate::config::Config;
use crate::parse::Cli;
use crate::platform::PlatformOps;
use crate::util::CommandDebugStdio;
use std::collections::HashSet;
use std::process::Stdio;
use std::{collections::HashMap, fs::File, io::BufReader, str::from_utf8};

pub struct PlatfromWl {
    cli: Cli,
    master_url: String,
    worker_url: String,
    upload_url: String,
    // gen_demos: HashSet<String>,
    config: Config,
}

impl PlatfromWl {
    pub fn new(cli: &Cli, config: Config) -> Self {
        let mut res = Self {
            cli: cli.clone(),
            master_url: "".to_owned(),
            worker_url: "".to_owned(),
            upload_url: "".to_owned(),
            // gen_demos: HashSet::new(),
            config: config.clone(),
        };

        let file = File::open(cli.cluster_config()).unwrap();
        let reader = BufReader::new(file);
        let config: HashMap<String, HashMap<String, serde_yaml::Value>> =
            serde_yaml::from_reader(reader).unwrap();

        let file = File::open(cli.cluster_config()).unwrap();
        let reader = BufReader::new(file);
        let config: HashMap<String, HashMap<String, serde_yaml::Value>> =
            serde_yaml::from_reader(reader).unwrap();

        for (_, conf) in config {
            if conf.contains_key("is_master") {
                res.master_url = format!(
                    "http://{}",
                    conf.get("ip").unwrap().as_str().unwrap().to_owned()
                );
            } else {
                res.worker_url = format!(
                    "http://{}",
                    conf.get("ip").unwrap().as_str().unwrap().to_owned()
                );
            }
        }

        // 设置 upload_url（根据您的需求调整）
        res.upload_url = format!("{}/upload", res.master_url);

        res
    }
}

impl PlatformOps for PlatfromWl {
    async fn prepare_apps_bin(&self, apps: Vec<String>, config: &Config) {
        let model_apps: Vec<String> = apps
            .clone()
            .into_iter()
            .filter(|app| config.models.contains_key(app))
            .collect();

        tracing::info!("prepare apps bin for {:?}", model_apps);

        for app in model_apps {
            let (_, _, mut child) = process::Command::new("python3")
                .args(&["../demos/scripts/1.gen_waverless_app.py", &app])
                .stdout(Stdio::piped())
                .stderr(Stdio::piped())
                .spawn_debug()
                .await;

            let res = child.wait().await.unwrap();
            // .status()
            // .await
            // .
            // .expect(&format!("Failed to gen demo {}", app));

            assert!(res.success(), "Failed to gen demo app: {}", app);
            // self.gen_demos.insert(demo.to_owned());
        }
    }

    fn cli(&self) -> &Cli {
        &self.cli
    }
    async fn remove_all_fn(&self) {}
    async fn upload_fn(&mut self, demo: &str, rename_sub: &str) {
        // if !self.gen_demos.contains(demo) {

        // }
        process::Command::new("python3")
            .args(&[
                "../middlewares/waverless/3.add_func.py",
                demo,
                rename_sub,
                &self.cli().cluster_config(),
            ])
            .status()
            .await
            .expect(&format!("Failed to add func {} as {}", demo, rename_sub));
    }
    async fn call_fn(
        &self,
        app: &str,
        func: &str,
        arg_json_value: &serde_json::Value,
        // _big_data: &Option<Vec<String>>,
    ) -> String {
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

    /// waverless embbed data storage
    /// - binded request data and big data in DataSet
    ///   https://fvd360f8oos.feishu.cn/wiki/M4ubwJkvcichuHkiGhjc0miHn5f#share-F0WBdFFhdop2ELxS3ZlcHWvZnD8
    /// - may panic if not support
    async fn write_data(
        &self,
        key: &str,
        arg_json_value: &serde_json::Value,
        data: &[u8],
    ) -> Option<String> {
        let client = reqwest::Client::new();
        let upload_endpoint = format!("{}/upload_data", self.master_url);
        let arg_json_str = serde_json::to_string(&arg_json_value).unwrap();
        let form = reqwest::multipart::Form::new()
            // request data part
            .part(
                key.to_owned(),
                reqwest::multipart::Part::bytes(arg_json_str.into_bytes()),
            )
            // big data part
            .part(
                "",
                reqwest::multipart::Part::bytes(data.to_vec()).file_name(""),
            );

        match client.post(&upload_endpoint).multipart(form).send().await {
            Err(e) => {
                panic!("上传请求发送失败: {:?}", e);
            }
            Ok(response) => {
                if !response.status().is_success() {
                    panic!("上传失败，响应 {:?}", response);
                } else {
                    tracing::info!("上传成功，状态码: {}", response.status());
                    if let Ok(text) = response.text().await {
                        // tracing::error!("错误信息: {}", text);
                        Some(text)
                    } else {
                        None
                    }
                }
            }
        }
    }
}
