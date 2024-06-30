# ### chdir
import os
import importlib.util
import inspect
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
file_path = '../pylib.py'
pylib = FunctionContainer()
load_functions_into_object(file_path, pylib)
#################################################################################################

pylib.key_step("Read Cluster Yaml")
config=pylib.read_yaml("cluster_config.yml")


pylib.key_step("Check Cluster Yaml")
ress=[]
for node in config:
    nodeconf=config[node]
    checks=[
        "waverless",
        "waverless1",
        "waverless2",
        ["k3s","k3s-agent"]
    ]
    res=""
    for check in checks:
        if type(check)==list:
            if "is_master" in nodeconf:
                check=check[0]
            else:
                check=check[1]
        # on=if pylib.cmd_check_service_status(c).remote("root",nodeconf["ip"]).run() "On" else "Off"
        on="Off"
        if pylib.cmd_check_service_status(check).remote("root",nodeconf["ip"]).run():
            on="On"

        res+=check+" "+on+", "
    ress.append(node+": "+res)
    
pylib.key_step("Print Res")
print("\n".join(ress))