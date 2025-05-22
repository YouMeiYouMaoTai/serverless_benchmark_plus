import yaml
import matplotlib.pyplot as plt
import numpy as np
import matplotlib as mpl
import platform
import os

def main():
    # 强制设置中文字体
    # 尝试多种方法来确保中文显示正常
    
    # 方法1: 设置多种可能的中文字体
    plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'WenQuanYi Micro Hei', 'Noto Sans CJK SC', 'DejaVu Sans']
    plt.rcParams['axes.unicode_minus'] = False
    
    # 方法2: 尝试直接读取系统字体
    try:
        # 使用matplotlib的字体管理器获取系统中存在的字体
        from matplotlib.font_manager import fontManager
        
        # 筛选包含中文支持的字体
        chinese_fonts = []
        for f in fontManager.ttflist:
            try:
                if os.path.exists(f.fname) and ('hei' in f.name.lower() or 
                                              'simkai' in f.name.lower() or 
                                              'micro' in f.name.lower() or 
                                              'cjk' in f.name.lower() or
                                              'wenquanyi' in f.name.lower()):
                    chinese_fonts.append(f.name)
            except:
                continue
                
        if chinese_fonts:
            # 找到中文字体，设置为优先使用
            print(f"找到中文字体: {chinese_fonts[0]}")
            plt.rcParams['font.sans-serif'].insert(0, chinese_fonts[0])
    except:
        print("尝试自动获取中文字体失败，请手动设置")
    
    # 方法3: 如果执行环境是Jupyter notebook，使用专用设置
    try:
        from IPython import get_ipython
        if get_ipython() is not None:
            mpl.rcParams['font.family'] = ['sans-serif']
            mpl.rcParams['font.sans-serif'] = ['SimHei', 'Arial Unicode MS']
    except:
        pass
    
    # 最后重建字体管理器
    mpl.font_manager._rebuild()
    
    # 不显示窗口
    plt.switch_backend('Agg')
    
    # 解析数据
    with open('perf.yaml', 'r') as file:
        data = yaml.safe_load(file)
    
    # 设置图表样式
    plt.style.use('ggplot')
    
    # 创建2x2的图表布局
    fig, axs = plt.subplots(2, 2, figsize=(18, 12))
    
    # 获取请求级别、系统和工作负载
    request_levels = ["low", "middle", "high"]
    systems = ["wl", "ow"]
    workloads = ["sequential", "parallel", "img_resize", "word_count"]
    
    # 为每个组合定义颜色
    # WL深色，OW浅色
    # 不同工作负载使用不同颜色
    colors = {
        ("wl", "sequential"): "darkblue",
        ("ow", "sequential"): "lightskyblue",
        ("wl", "parallel"): "darkgreen",
        ("ow", "parallel"): "lightgreen",
        ("wl", "img_resize"): "darkred",
        ("ow", "img_resize"): "lightcoral",
        ("wl", "word_count"): "darkorchid",
        ("ow", "word_count"): "plum"
    }
    
    # 设置X轴位置
    x = np.arange(len(request_levels))
    width = 0.11  # 较窄的条形宽度以适应8个条形
    
    # 按工作负载和平台定义位置偏移
    offsets = {
        ("wl", "sequential"): -3.5 * width,
        ("ow", "sequential"): -2.5 * width,
        ("wl", "parallel"): -1.5 * width,
        ("ow", "parallel"): -0.5 * width,
        ("wl", "img_resize"): 0.5 * width,
        ("ow", "img_resize"): 1.5 * width,
        ("wl", "word_count"): 2.5 * width,
        ("ow", "word_count"): 3.5 * width
    }
    
    # 图例标签
    labels = {
        ("wl", "sequential"): "WL - 顺序执行",
        ("ow", "sequential"): "OW - 顺序执行",
        ("wl", "parallel"): "WL - 并行执行",
        ("ow", "parallel"): "OW - 并行执行",
        ("wl", "img_resize"): "WL - 图像缩放",
        ("ow", "img_resize"): "OW - 图像缩放",
        ("wl", "word_count"): "WL - 词频统计",
        ("ow", "word_count"): "OW - 词频统计"
    }
    
    # 中文X轴标签
    x_labels = ['低负载', '中负载', '高负载']
    
    # 0. 总时间 (左上角)
    for system in systems:
        for workload in workloads:
            combo = (system, workload)
            total_times = []
            for level in request_levels:
                exec_time = data.get(level, {}).get(system, {}).get(workload, {}).get("avg_exec_time", 0)
                cold_time = data.get(level, {}).get(system, {}).get(workload, {}).get("avg_coldtrans_time", 0)
                total_times.append(exec_time + cold_time)
            
            axs[0, 0].bar(x + offsets[combo], total_times, width, 
                      label=labels[combo], color=colors[combo])
    
    axs[0, 0].set_title('总时间 (执行 + 冷启动)', fontsize=14)
    axs[0, 0].set_ylabel('时间 (毫秒)', fontsize=12)
    axs[0, 0].set_xticks(x)
    axs[0, 0].set_xticklabels(x_labels)
    axs[0, 0].legend(fontsize=8, ncol=2)
    
    # 1. 平均执行时间 (右上角)
    for system in systems:
        for workload in workloads:
            combo = (system, workload)
            exec_times = [data.get(level, {}).get(system, {}).get(workload, {}).get("avg_exec_time", 0) for level in request_levels]
            axs[0, 1].bar(x + offsets[combo], exec_times, width, 
                       label=labels[combo], color=colors[combo])
    
    axs[0, 1].set_title('平均执行时间', fontsize=14)
    axs[0, 1].set_ylabel('时间 (毫秒)', fontsize=12)
    axs[0, 1].set_xticks(x)
    axs[0, 1].set_xticklabels(x_labels)
    axs[0, 1].legend(fontsize=8, ncol=2)
    
    # 2. 冷启动时间 (左下角)
    for system in systems:
        for workload in workloads:
            combo = (system, workload)
            cold_times = [data.get(level, {}).get(system, {}).get(workload, {}).get("avg_coldtrans_time", 0) for level in request_levels]
            axs[1, 0].bar(x + offsets[combo], cold_times, width, 
                       label=labels[combo], color=colors[combo])
    
    axs[1, 0].set_title('冷启动时间', fontsize=14)
    axs[1, 0].set_ylabel('时间 (毫秒)', fontsize=12)
    axs[1, 0].set_xticks(x)
    axs[1, 0].set_xticklabels(x_labels)
    axs[1, 0].legend(fontsize=8, ncol=2)
    
    # 3. 每秒请求数 (右下角)
    for system in systems:
        for workload in workloads:
            combo = (system, workload)
            rps_values = [data.get(level, {}).get(system, {}).get(workload, {}).get("rps", 0) for level in request_levels]
            axs[1, 1].bar(x + offsets[combo], rps_values, width, 
                       label=labels[combo], color=colors[combo])
    
    axs[1, 1].set_title('每秒请求数 (RPS)', fontsize=14)
    axs[1, 1].set_ylabel('每秒请求数', fontsize=12)
    axs[1, 1].set_xticks(x)
    axs[1, 1].set_xticklabels(x_labels)
    axs[1, 1].legend(fontsize=8, ncol=2)
    
    # 添加总标题并调整布局
    fig.suptitle('性能对比', fontsize=16)
    plt.tight_layout(rect=[0, 0, 1, 0.95])  # 为总标题留出空间
    plt.savefig('performance_comparison.png', dpi=300)
    print("图表已保存至 'performance_comparison.png'")

if __name__ == "__main__":
    main() 