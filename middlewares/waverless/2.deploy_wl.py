# ### chdir
import os
import importlib.util
import inspect
import yaml
CUR_FPATH = os.path.abspath(__file__)
CUR_FDIR = os.path.dirname(CUR_FPATH)
# chdir to the directory of this script
os.chdir(CUR_FDIR)
class FunctionContainer:
    pass
def load_functions_into_object(file_path, obj):
    # 从指定文件路径导入模块
    spec = importlib.util.spec_from_file_location("module.name", file_path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    # 获取模块中定义的所有函数
    functions = {name: obj for name, obj in inspect.getmembers(module) if inspect.isfunction(obj)}
    # 将函数添加到对象中
    for func_name, func in functions.items():
        setattr(obj, func_name, func)
# 使用示例
file_path = '../../pylib.py'
pylib = FunctionContainer()
load_functions_into_object(file_path, pylib)
#################################################################################################


pylib.key_step("Clone Waverless")
os.system("git clone https://github.com/340Lab/waverless")


def copy_node_config():
    yamlformat="""
    nodes: 
    1: 
        addr: 192.168.31.162:2500
        spec: [meta,master]
    2: 
        addr: 192.168.31.87:2500
        spec: [meta,worker]
    3: 
        addr: 192.168.31.96:2500
        spec: [meta,worker]
    4: 
        addr: 192.168.31.109:2500
        spec: [meta,worker]
    5: 
        addr: 192.168.31.56:2500
        spec: [meta,worker]

    loki:
    addr: 192.168.31.56:3100
    """
    srcyaml=pylib.read_yaml("../cluster_config.yml")
    yamlobj={"nodes":{}}
    for n in srcyaml:
        spec=["meta","worker"]
        if "is_master" in srcyaml[n]:
            spec[1]="master"
        nnum=int(n[3:])
        yamlobj["nodes"][nnum]={"addr":srcyaml[n]["ip"]+":2500","spec":spec}
    with open("waverless/scripts/deploy_cluster/node_config.yaml", 'w') as f:
        yaml.dump(yamlobj, f)
    # print file
    print(">> node_config file created")
    with open("waverless/scripts/deploy_cluster/node_config.yaml", 'r') as f:
        print(f.read())
pylib.key_step(copy_node_config)


# see waverless/scripts/deploy_cluster/deploy_config_tmp.yaml
def gen_deploy_conf():
    srcyaml=pylib.read_yaml("../cluster_config.yml")
    yamlobj={}
    for n in srcyaml:
        nconf=srcyaml[n]
        yamlobj[nconf["ip"]+":22"]={"user":"root"}
    with open("waverless/scripts/deploy_cluster/deploy_config.yml", 'w') as f:
        yaml.dump(yamlobj, f)
    print(">> deploy_config file created")
    with open("waverless/scripts/deploy_cluster/deploy_config.yml", 'r') as f:
        print(f.read())

pylib.key_step(gen_deploy_conf)
# # pylib.os_system_sure("cp ../cluster_config.yml waverless/scripts/deploy_cluster/node_config.yaml")


# def setup_ssh():
#     pylib.os_system_sure("python3 waverless/scripts/deploy_cluster/0.setup_ansible_ssh.py")
# pylib.key_step(setup_ssh)



# # pylib.key_step("Install remove env")
# def install_remote_env():
#     pylib.os_system_sure("python3 waverless/scripts/deploy_cluster/1.install_remote_env.py")
# pylib.key_step(install_remote_env)

# pylib.key_step("Pack prj")
def pack_local():
    pylib.os_system_sure("python3 waverless/scripts/deploy_cluster/2.pack_local.py")
pylib.key_step(pack_local)
# pylib.os_system_sure("python3 waverless/scripts/deploy_cluster/2.pack_local.py")


# pylib.key_step("Deploy")
def deploy():
    pylib.os_system_sure("python3 waverless/scripts/deploy_cluster/3.deploy.py")
pylib.key_step(deploy)