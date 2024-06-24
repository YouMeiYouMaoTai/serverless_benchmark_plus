### chdir
import os
import sys
import yaml
import zipfile
import re
import xml.etree.ElementTree as ET

CUR_FPATH = os.path.abspath(__file__)
CUR_FDIR = os.path.dirname(CUR_FPATH)
# chdir to the directory of this script
os.chdir(CUR_FDIR)

# os.system('ansible-playbook -vv 2.ans_install_build.yml -i ../local_ansible_conf.ini')
### utils
def os_system_sure(command):
    print(f"Run：{command}\n")
    result = os.system(command)
    if result != 0:
        print(f"\nFail：{command}\n\n")
        exit(1)
    print(f"\nSucc：{command}\n\n")


# result.returncode
# result.stdout
def run_cmd_return(cmd):
    print(f"Run：{cmd}\n")
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    print(f"\nStdout：{result.stdout}\n\n")
    return result


def print_title(title):
    print(f"\n\n>>> {title}")


def find_folders_recursively(directory,target:str):
    folders = []
    
    # 遍历目录及其子目录
    for root, dirs, files in os.walk(directory):
        for dir in dirs:
            if dir == target:
                folders.append(os.path.join(root, dir))
            # folders.append(os.path.join(root, dir))
    return folders

def bigcamel_to_snake(name):
    # 将大驼峰命名转换为蛇形命名
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    snake_case = re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()
    return snake_case

def add_dependency_to_pom(file_path, group_id, artifact_id, version=None):
    # 解析pom.xml文件
    ET.register_namespace('', 'http://maven.apache.org/POM/4.0.0')
    tree = ET.parse(file_path)
    root = tree.getroot()

    # 找到<dependencies>标签
    dependencies = root.find('{http://maven.apache.org/POM/4.0.0}dependencies')
    if dependencies is None:
        # 如果<dependencies>标签不存在，则创建一个新的
        dependencies = ET.SubElement(root, 'dependencies')

    # 创建新的<dependency>标签
    dependency = ET.SubElement(dependencies, 'dependency')
    
    group_id_element = ET.SubElement(dependency, 'groupId')
    group_id_element.text = group_id
    
    artifact_id_element = ET.SubElement(dependency, 'artifactId')
    artifact_id_element.text = artifact_id
    
    if version:
        version_element = ET.SubElement(dependency, 'version')
        version_element.text = version
    else:
        # 注释掉<version>标签
        version_element = ET.Comment(' <version>0.0.1-SNAPSHOT</version> ')
        dependency.append(version_element)

    # 将修改后的内容写回pom.xml文件
    tree.write(file_path, encoding='utf-8', xml_declaration=True)
    

def snake_to_big_camel(snake_str):
    # 使用split将蛇形命名拆分成单词列表
    components = snake_str.split('_')
    # 将每个单词的首字母大写并连接成一个字符串
    return ''.join(x.title() for x in components)
#################################################################################################


if len(sys.argv)!=2:
    print("Usage: python3 1.gen_waverless_prj.py <project_name>")
    exit(1)

prj = sys.argv[1]

temp_prj_dir="waverless/"+prj
os_system_sure(f"rm -rf {temp_prj_dir}")
os_system_sure(f"mkdir -p {temp_prj_dir}")

## copy prj
os_system_sure(f"cp -r ../{prj} ./waverless/")

## construct app config file for waverless
#  find functions dir in prj
functions_dir = find_folders_recursively(f"./waverless/{prj}","functions")[0]
print(functions_dir)
app_yml={prj:{}}
#  for each XXX.java, construct app.yml
for fnfile in os.listdir(functions_dir):
    if fnfile.endswith(".java"):
        fnname = bigcamel_to_snake(fnfile.split(".")[0])
        app_yml[prj][fnname]={
            "rpc": None
        }
        # app_yml = f"""name: {fnname}
#  encode to file
with open(f"{temp_prj_dir}/app.yml","w") as f:
    yaml.dump(app_yml,f)

## gen adapt codes
#  pom.xml
"""
<dependency>
    <groupId>io.serverless_lib</groupId>
    <artifactId>serverless-lib-core</artifactId>
    <!-- <version>0.0.1-SNAPSHOT</version> -->
</dependency>
"""
# parse pom.xml
add_dependency_to_pom(f"{temp_prj_dir}/pom.xml","io.serverless_lib","serverless-lib-core","0.0.1-SNAPSHOT")


#  Application.java
functions_parent_dir = os.path.dirname(functions_dir)
package_name=functions_parent_dir.split("main/java/")[-1].replace("/",".")
application_java=f"""
package {package_name};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {{"{package_name}","io.serverless_lib"}})
public class Application {{
    public static void main(String[] args) {{
        SpringApplication.run(Application.class, args);
    }}
}}
"""
with open(f"{functions_parent_dir}/Application.java","w") as f:
    f.write(application_java)


#  ServiceDispatcher.java
emmbed_fns=""
for fn in app_yml[prj]:
    emmbed_fns+=f"""
    @Autowired
    private {snake_to_big_camel(fn)} {fn};
    public JsonObject {fn}(JsonObject arg){{
        return {fn}.call(arg);
    }}
    """
import_fns="".join([f"import {package_name}.functions.{snake_to_big_camel(fn)};\n" for fn in app_yml[prj]])

service_dispatcher_java= f"""
package {package_name};

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import com.google.gson.JsonObject;
import io.serverless_lib.RpcHandleOwner;
{import_fns}


@Service
public class ServiceDispatcher {{
    //@Autowired
    //private Fn1 fn1;

    @Autowired
    RpcHandleOwner rpcHandleOwner;

    @PostConstruct
    public void init() {{
        rpcHandleOwner.register((ServiceDispatcher) this);
    }}

    //public JsonObject fn1(JsonObject arg){{
    //    return fn1.call(arg);
    //}}

    {emmbed_fns}    
}}
"""
with open(f"{functions_parent_dir}/ServiceDispatcher.java","w") as f:
    f.write(service_dispatcher_java)


os.chdir(temp_prj_dir)
os_system_sure("mvn clean package")