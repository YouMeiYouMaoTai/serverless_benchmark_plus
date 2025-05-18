use std::sync::Arc;

use crate::{
    config::{Config, FnDetails},
    parse::Cli,
    platform_ow, platform_wl,
};
use enum_dispatch::enum_dispatch;

#[enum_dispatch(PlatformOps)]
pub enum PlatformOpsBind {
    PlatfromOw(platform_ow::PlatfromOw),
    PlatfromWl(platform_wl::PlatfromWl),
}

impl PlatformOpsBind {
    // return if need call fn, or just triggered by data written
    pub async fn bf_call_fn(
        &self,
        app: &str,
        func: &str,
        arg_json_value: &serde_json::Value,
        big_data: &Option<Vec<String>>,
        fn_details: &FnDetails,
    ) -> Option<String> {
        // write big data
        let trigger_fn_res = fn_details
            .write_big_data(app, func, arg_json_value, self)
            .await;

        // self.call_fn(
        //     &fn_details.app,
        //     &fn_details.func,
        //     &serde_json::to_value(fn_details.args).unwrap(),
        // )

        // use minio need explicit call fn
        trigger_fn_res
    }
}

#[enum_dispatch]
pub trait PlatformOps: Send + 'static {
    fn cli(&self) -> &Cli;
    async fn remove_all_fn(&self);
    async fn upload_fn(&mut self, demo: &str, rename_sub: &str);
    async fn call_fn(
        &self,
        app: &str,
        func: &str,
        arg_json_value: &serde_json::Value,
        // fn_details: &FnDetails,
        // big_data: &Option<Vec<String>>,
    ) -> String;
    async fn prepare_apps_bin(&self, apps: Vec<String>, config: &Config);

    /// waverless embbed data storage
    /// - binded request data and big data in DataSet
    ///   https://fvd360f8oos.feishu.cn/wiki/M4ubwJkvcichuHkiGhjc0miHn5f#share-F0WBdFFhdop2ELxS3ZlcHWvZnD8
    /// - may panic if not support
    /// - return sub trigger fn result if triggered
    async fn write_data(
        &self,
        key: &str,
        arg_json_value: &serde_json::Value,
        data: &[u8],
    ) -> Option<String>;
}
