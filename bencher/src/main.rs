mod img_resize;

use std::path::PathBuf;
use std::process;

use clap::arg;
use clap::value_parser;
use clap::{command, Command};
use clap::{Parser, Subcommand};
use goose::prelude::*;
use s3::creds::Credentials;
use s3::Bucket;
use s3::BucketConfiguration;
use s3::Region;
use std::sync::mpsc;
use tokio::sync::oneshot;
use tokio::sync::Mutex;

#[derive(Parser)]
#[command(version, about, long_about = None)]
struct Cli {
    // #[arg(action = clap::ArgAction::Count)]
    #[arg(long, action = clap::ArgAction::Count)]
    img_resize: u8,

    #[arg(long, action = clap::ArgAction::Count)]
    with_ow: u8,

    #[arg(long, action = clap::ArgAction::Count)]
    with_wl: u8,
}

lazy_static::lazy_static! {
    pub static ref BUCKET:Bucket={
        let(tx,rx)=mpsc::channel();
        tokio::spawn(async move{
            let bucket_name="serverless-bench";
            let region=Region::Custom {
                region: "eu-central-1".to_owned(),
                endpoint: "http://localhost:9009".to_owned(),
            };
            let credentials= Credentials {
                access_key: Some("minioadmin".to_owned()),
                secret_key: Some("minioadmin123".to_owned()),
                security_token: None,
                session_token: None,
                expiration: None,
            };

            let mut bucket=Bucket::new(bucket_name,region.clone(), credentials.clone()).unwrap().with_path_style();

            if bucket.exists().await.unwrap() {
                for b in bucket.list("".to_owned(),None).await.unwrap(){
                    bucket.delete_object(b.name).await.unwrap();
                    // bucket.delete().await.unwrap();
                }
            }else{
                bucket = Bucket::create_with_path_style(
                    bucket_name,
                    region,
                    credentials,
                    BucketConfiguration::default(),
                )
                .await.unwrap()
                .bucket;
            }

            tx.send(bucket);
        });
        rx.recv().unwrap()
    };
}

#[tokio::main]
async fn main() -> Result<(), GooseError> {
    // don't go thouph proxy when performance
    std::env::remove_var("http_proxy");
    std::env::remove_var("https_proxy");

    let cli = Cli::parse();

    assert!(
        !(cli.with_ow > 0 && cli.with_wl > 0),
        "Cannot run with both OpenWhisk and Waverless"
    );

    if cli.with_ow > 0 {
        // run python3 ../middlewares/openwhisk/7.clean_all_fns.py
        process::Command::new("python3")
            .args(&["../middlewares/openwhisk/7.clean_all_fns.py"])
            .status()
            .expect("Failed to execute python3 ../middlewares/openwhisk/7.clean_all_fns.py");

        // run python3 ../middlewares/openwhisk/8.add_func.py <func>
        if cli.img_resize > 0 {
            process::Command::new("python3")
                .args(&["../middlewares/openwhisk/8.add_func.py", "img_resize"])
                .status()
                .expect(
                    "Failed to execute python3 ../middlewares/openwhisk/8.add_func.py img_resize",
                );
        }
    }

    if cli.img_resize > 0 {
        // docker compose up -d at ../middlewares/minio/docker-compose.yaml
        // process::Command::new("docker-compose")
        //     .args(&["down"])
        //     .current_dir("../middlewares/minio")
        //     .status()
        //     .expect("Failed to execute docker-compose down");
        // process::Command::new("docker-compose")
        //     .args(&["up", "-d"])
        //     .current_dir("../middlewares/minio")
        //     .status()
        //     .expect("Failed to execute docker-compose up -d");
    }

    // let mut bucket = BUCKET.lock().await;

    let seed = "helloworld";
    img_resize::prepare(seed);

    if cli.img_resize > 0 {
        img_resize::test_call(cli).await;
        return Ok(());
    }

    // GooseAttack::initialize()?
    //     .register_scenario(
    //         scenario!("LoadtestTransactions").register_transaction(transaction!(img_resize)),
    //     )
    //     .execute()
    //     .await?;

    Ok(())
}
