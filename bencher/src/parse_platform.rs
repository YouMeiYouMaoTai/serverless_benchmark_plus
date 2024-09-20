use crate::parse::Cli;

pub enum Platform {
    OpenWhisk,
    Waverless,
    Raw,
}

impl From<&Cli> for Platform {
    fn from(cli: &Cli) -> Self {
        if cli.with_ow > 0 {
            Platform::OpenWhisk
        } else if cli.with_wl > 0 {
            Platform::Waverless
        } else {
            Platform::Raw
        }
    }
}

impl ToString for Platform {
    fn to_string(&self) -> String {
        match self {
            Platform::OpenWhisk => "ow".to_owned(),
            Platform::Waverless => "wl".to_owned(),
            Platform::Raw => "raw".to_owned(),
        }
    }
}
