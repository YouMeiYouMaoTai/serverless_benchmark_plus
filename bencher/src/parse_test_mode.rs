use crate::parse::Cli;

pub enum TestMode {
    Once,
    FirstCall,
    Bench,
}

impl From<&Cli> for TestMode {
    fn from(cli: &Cli) -> Self {
        if cli.bench_mode > 0 {
            TestMode::Bench
        } else if cli.first_call_mode > 0 {
            TestMode::FirstCall
        } else {
            TestMode::Once
        }
    }
}

impl ToString for TestMode {
    fn to_string(&self) -> String {
        match self {
            TestMode::Once => "once".to_owned(),
            TestMode::FirstCall => "first_call".to_owned(),
            TestMode::Bench => "bench".to_owned(),
        }
    }
}
