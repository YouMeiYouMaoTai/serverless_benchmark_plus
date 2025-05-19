use serde_json;
use serde_yaml;
use std::collections::BTreeMap;

/// Converts a serde_json::Value into a serde_yaml::Value
pub fn json_to_yaml(json_value: serde_json::Value) -> serde_yaml::Value {
    match json_value {
        serde_json::Value::Null => serde_yaml::Value::Null,
        serde_json::Value::Bool(b) => serde_yaml::Value::Bool(b),
        serde_json::Value::Number(n) => {
            if let Some(i) = n.as_i64() {
                serde_yaml::Value::Number(i.into())
            } else if let Some(u) = n.as_u64() {
                serde_yaml::Value::Number(u.into())
            } else if let Some(f) = n.as_f64() {
                serde_yaml::Value::Number(f.into())
            } else {
                // This should never happen with valid JSON numbers
                serde_yaml::Value::Null
            }
        }
        serde_json::Value::String(s) => serde_yaml::Value::String(s),
        serde_json::Value::Array(arr) => {
            let yaml_arr = arr.into_iter().map(json_to_yaml).collect();
            serde_yaml::Value::Sequence(yaml_arr)
        }
        serde_json::Value::Object(obj) => {
            let mut yaml_map = serde_yaml::Mapping::new();
            for (key, value) in obj {
                yaml_map.insert(serde_yaml::Value::String(key), json_to_yaml(value));
            }
            serde_yaml::Value::Mapping(yaml_map)
        }
    }
}

/// Converts a serde_yaml::Value into a serde_json::Value
pub fn yaml_to_json(yaml_value: serde_yaml::Value) -> serde_json::Value {
    match yaml_value {
        serde_yaml::Value::Null => serde_json::Value::Null,
        serde_yaml::Value::Bool(b) => serde_json::Value::Bool(b),
        serde_yaml::Value::Number(n) => {
            if let Some(i) = n.as_i64() {
                serde_json::Value::Number(i.into())
            } else if let Some(f) = n.as_f64() {
                // JSON doesn't have a dedicated u64 representation, so we convert to f64
                if let Some(n) = serde_json::Number::from_f64(f) {
                    serde_json::Value::Number(n)
                } else {
                    serde_json::Value::Null
                }
            } else {
                serde_json::Value::Null
            }
        }
        serde_yaml::Value::String(s) => serde_json::Value::String(s),
        serde_yaml::Value::Sequence(seq) => {
            let json_arr = seq.into_iter().map(yaml_to_json).collect();
            serde_json::Value::Array(json_arr)
        }
        serde_yaml::Value::Mapping(map) => {
            let mut json_obj = serde_json::Map::new();
            for (key, value) in map {
                // In YAML, keys can be any value, but in JSON they must be strings
                // We'll convert keys to strings, or use their debug representation if needed
                let key_str = match key {
                    serde_yaml::Value::String(s) => s,
                    _ => format!("{:?}", key),
                };
                json_obj.insert(key_str, yaml_to_json(value));
            }
            serde_json::Value::Object(json_obj)
        }
        serde_yaml::Value::Tagged(_) => panic!("Tagged value not supported"),
    }
}
