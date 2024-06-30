use super::Cli;
use base64::encode;
use image::io::Reader as ImageReader;
use image::{ImageBuffer, RgbImage};
use rand::{Rng, SeedableRng};
use reqwest::Response;
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::fmt::{format, Debug};
use std::fs::{self, File};
use std::io::BufReader;
use std::path::Path;
use std::process::Command as Process;

use crate::BUCKET;

#[derive(Serialize, Deserialize, Debug)]
struct Args {
    image_s3_path: String,
    target_width: i32,
    target_height: i32,
}

#[derive(Serialize, Deserialize, Debug)]
struct Resp {
    resized_image: String,
}

#[derive(Debug)]
enum CallRes<R: Debug> {
    ProcessOut(std::process::Output),
    Struct(R),
}

pub async fn test_call(cli: Cli) {
    // read image from file
    let img = fs::read("img_resize/image_0.jpg").unwrap();

    BUCKET
        // .lock()
        // .await
        .put_object(format!("image_{}.jpg", 0), &img)
        .await
        .unwrap();

    let arg = Args {
        image_s3_path: format!("image_{}.jpg", 0),
        target_width: 50,
        target_height: 50,
    };
    let output = if cli.with_ow > 0 {
        // wsk action invoke --result img_resize_resize --param <json>
        let arg_value = serde_json::to_value(&arg).unwrap();
        let mut p = Process::new("wsk");
        let args = vec!["action", "invoke", "--result", "img_resize_resize"];
        p.args(&args);
        // p.arg("--param");
        // p.arg(&serde_json::to_string(&arg).unwrap());
        // p.arg("dummy_arg");

        for (k, v) in arg_value.as_object().unwrap() {
            p.arg("--param");
            p.arg(k);
            p.arg(&match v {
                serde_json::Value::String(s) => s.to_owned(),
                _ => v.to_string(),
            });
        }

        // debug p cmd
        println!("{:?}", p);

        CallRes::ProcessOut(p.output().expect("Failed to execute wsk action invoke"))
    } else if cli.with_wl > 0 {
        let res: Resp = reqwest::Client::new()
            .post("http://192.168.31.54:2501/img_resize/resize")
            .body(serde_json::to_string(&arg).unwrap())
            .send()
            .await
            .unwrap_or_else(|e| panic!("err: {:?}", e))
            .json()
            .await
            .unwrap();
        CallRes::Struct(res)
    } else {
        let argstr = serde_json::to_string(&arg).unwrap();
        let p=Process::new("java")
        .args(&[
            "-jar",
            "../demos/scripts/ow/img_resize/resize/target/hello-1.0-SNAPSHOT-jar-with-dependencies.jar",
            &argstr
        ]).output()
        .expect("Failed to execute wsk action invoke");
        CallRes::ProcessOut(p)
    };

    println!("debug output: {:?}", output);
    let res: Resp = match output {
        CallRes::ProcessOut(output) => {
            serde_json::from_slice(&output.stdout).expect("Failed to parse JSON response")
        }
        CallRes::Struct(res) => res,
    };
    let res = BUCKET.get_object(&res.resized_image).await.unwrap();
    std::fs::write("resized_image.jpg", res.as_slice()).unwrap();
}

// pub async fn test(user: &mut GooseUser) -> TransactionResult {
//     let _goose_metrics = user.get("").await?;

//     Ok(())
// }

// 将字符串转换为u64哈希值
fn hash_str(s: &str) -> u64 {
    use std::hash::{Hash, Hasher};
    let mut hasher = std::collections::hash_map::DefaultHasher::new();
    s.hash(&mut hasher);
    hasher.finish()
}

pub fn prepare(seed: &str) {
    // mkdir
    if fs::metadata("img_resize").is_ok() {
        return;
    }
    let _ = fs::create_dir_all("img_resize");

    // 使用种子初始化随机数生成器
    let mut rng = rand::rngs::StdRng::seed_from_u64(hash_str(seed));

    for i in 0..100 {
        // 生成随机宽度和高度
        let width = rng.gen_range(100..1000);
        let height = rng.gen_range(100..1000);

        // 创建空白图片
        let mut img: RgbImage = ImageBuffer::new(width, height);

        // 填充图片的每个像素
        for x in 0..width {
            for y in 0..height {
                let pixel = img.get_pixel_mut(x, y);
                *pixel = image::Rgb([rng.gen(), rng.gen(), rng.gen()]);
            }
        }

        img.save_with_format(
            format!("img_resize/image_{}.jpg", i),
            image::ImageFormat::Jpeg,
        )
        .unwrap();
    }
}
