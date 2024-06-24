
package test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import java.io.PrintStream;
import javax.annotation.PostConstruct;
import test.functions.Resize;

class NullOutputStream extends java.io.OutputStream {
    @Override
    public void write(int b) {
        // 不做任何处理
    }
}

@Component
class Entrypoint {
    @Autowired
    private Resize resize;

    @PostConstruct
    public void init() {
        // parse json from arg[0]
        // 创建JsonParser对象
        JsonParser parser = new JsonParser();

        // 将JSON字符串解析为JsonObject
        JsonObject arg = parser.parse(Application.arg).getAsJsonObject();
        JsonObject resp = resize.call(arg);

        Application.out.println(resp.toString());
    }
}

@SpringBootApplication
@ComponentScan(basePackages = {"test"})
public class Application {
    public static PrintStream out=null;
    public static String arg=null;
    public static void main(String[] args) {
        out=System.out;
        arg=args[0];
        // 禁用System.out
        System.setOut(new PrintStream(new NullOutputStream()));
        // 禁用System.err
        System.setErr(new PrintStream(new NullOutputStream()));

        SpringApplication.run(Application.class, args);
    }
}
