use lazy_static::lazy_static;
use serde_json::Value;
use std::collections::HashMap;
use std::sync::atomic::{AtomicUsize, Ordering};

use crate::util_serde::yaml_to_json;

lazy_static! {
    static ref REQUEST_ID_COUNTER: AtomicUsize = AtomicUsize::new(1);
}

// new request arg and request id
pub fn prepare_once_call_arg(
    src_arg: &HashMap<String, serde_yaml::Value>,
) -> (serde_json::Value, usize) {
    // Get the next request ID
    let request_id = REQUEST_ID_COUNTER.fetch_add(1, Ordering::SeqCst);
    // .to_string();

    // Create a new map to store the result
    let mut result_map = serde_json::Map::new();

    // Process each key-value pair
    for (key, value) in src_arg {
        // Replace ${REQUEST_ID} in the key if present
        let processed_key = if key.contains("${REQUEST_ID}") {
            key.replace("${REQUEST_ID}", &request_id.to_string())
        } else {
            key.clone()
        };

        // Process the value based on its type
        let processed_value: serde_json::Value = match value {
            serde_yaml::Value::String(s) => {
                if s.contains("${REQUEST_ID}") {
                    // Only replace placeholder in string values
                    serde_json::Value::String(s.replace("${REQUEST_ID}", &request_id.to_string()))
                } else {
                    // Keep unchanged string values
                    serde_json::Value::String(s.to_owned())
                }
            }
            // For non-string values, preserve the original value
            yamlval => serde_json::Value::String(yaml_to_json(yamlval.clone()).to_string()),
        };

        // Add the processed key-value pair to the result
        result_map.insert(processed_key, processed_value);
    }

    // Convert the map to a JSON Value::Object
    (Value::Object(result_map), request_id)
}
