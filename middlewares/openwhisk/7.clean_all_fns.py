import os, yaml
CUR_FPATH = os.path.abspath(__file__)
CUR_FDIR = os.path.dirname(CUR_FPATH)
# chdir to the directory of this script
os.chdir(CUR_FDIR)


import os
def os_system_sure(command):
    print(f"执行命令：{command}")
    result = os.system(command)
    if result != 0:
        print(f"命令执行失败：{command}")
        exit(1)
    print(f"命令执行成功：{command}\n\n")


def run_cmd_with_res(cmd):
    print(f"执行命令：{cmd}")
    result = os.popen(cmd)
    # print(f"执行结果：{result.read()}")
    return result.read()

fns=run_cmd_with_res("wsk -i list").split("\n")

def filter_fn(fn):
    if fn.startswith("/"):
        return True
    return False
fns=filter(filter_fn,fns)
fns=[fn.split(" ")[0] for fn in fns]

print(fns)

cnt=0
for fn in fns:
    os_system_sure(f"wsk action delete {fn}")
    cnt+=1

print("\n>>> Deleted",cnt,"functions.")
