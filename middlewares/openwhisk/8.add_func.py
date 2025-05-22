import glob
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

def os_system(command):
    print(f"执行命令：{command}")
    result = os.system(command)
    if result != 0:
        print(f"命令执行失败：{command}")
    else:
        print(f"命令执行成功：{command}\n\n")

def run_cmd_with_res(cmd):
    print(f"执行命令：{cmd}")
    result = os.popen(cmd)
    # print(f"执行结果：{result.read()}")
    return result.read()


import sys
if len(sys.argv) !=3:
    print("Usage: python 8.add_func.py <demo_app> <rename_sub>")
    exit(1)

demo_app=sys.argv[1]
rename=demo_app+sys.argv[2]
# targetname=sys.argv[2]

os.chdir(f"../../demos")
os_system_sure(f"python3 scripts/1.gen_ow_app.py {demo_app}")


ow_app_dir=os.path.abspath(f"scripts/ow/{demo_app}")

# get fnlist
fnlist=os.listdir(ow_app_dir)

print(f">>> find funcs in {ow_app_dir}: {fnlist}")
    
for fn in fnlist:
    # list funcs in ow_app_dir
    file_pattern = os.path.join(ow_app_dir, 
        fn,'target','*with-dependencies*')
    glob_results = glob.glob(file_pattern)


    print(f">>> find jar files in {ow_app_dir}: {glob_results}")
    first_jar_file = glob_results[0]
    print(f">>> we use the first jar file: {first_jar_file}")

    print(f"uploading file {first_jar_file} as app/fn {demo_app}/{fn}")
    os_system(f"wsk action delete {demo_app}_{fn}")
    # -c concurrency 100 in container
    os_system_sure(f"wsk -i action create {demo_app}_{fn} {first_jar_file} --main test.Application")

# list funcs
os_system_sure("wsk list")
