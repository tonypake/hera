package com.dfire.core.job;

import com.alibaba.fastjson.JSONObject;
import com.dfire.common.constants.RunningJobKeyConstant;
import com.dfire.core.util.CommandUtils;
import com.dfire.logs.HeraLog;
import com.dfire.logs.TaskLog;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">凌霄</a>
 * @time: Created in 上午12:30 2018/4/26
 * @desc shell脚本执行类，拼接shell文件，执行文件执行命令
 */

public class ShellJob extends ProcessJob {

    private String shell = null;

    public ShellJob(JobContext jobContext) {
        super(jobContext);
    }

    /**
     * 脚本执行命令集合
     * 主要包括：切换用户，修改文件权限，执行制定脚本
     *
     * @return 命令集合
     */
    @Override
    public List<String> getCommandList() {
        String script;
        if (shell != null) {
            script = shell;
        } else {
            script = getProperties().getLocalProperty(RunningJobKeyConstant.JOB_SCRIPT);
        }
        OutputStreamWriter outputStreamWriter = null;
        try {
            File f = new File(jobContext.getWorkDir() + File.separator + (System.currentTimeMillis()) + ".sh");
            if (!f.exists()) {
                if (!f.createNewFile()) {
                    log("ERROR:创建文件失败 " + f.getAbsolutePath());
                    HeraLog.error("创建文件失败:" + f.getAbsolutePath());
                }
            }
            outputStreamWriter = new OutputStreamWriter(new FileOutputStream(f), Charset.forName("utf-8"));
            outputStreamWriter.write(script);
            getProperties().setProperty(RunningJobKeyConstant.RUN_SHELL_PATH, f.getAbsolutePath());

        } catch (IOException e) {
            jobContext.getHeraJobHistory().getLog().appendHeraException(e);
        } finally {
            if (outputStreamWriter != null) {
                try {
                    outputStreamWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        String shellFilePath = getProperty(RunningJobKeyConstant.RUN_SHELL_PATH, "");
        List<String> list = new ArrayList<>();
        //修改权限
        String shellPrefix = getJobPrefix();
        //过滤不需要转化的后缀名
        boolean isDocToUnix = checkDosToUnix(shellFilePath);
        if (isDocToUnix) {
            list.add("dos2unix " + shellFilePath);
            log("dos2unix file:" + shellFilePath);
        }

        if (StringUtils.isNotBlank(shellPrefix)) {
            String tmpFilePath = jobContext.getWorkDir() + File.separator + "tmp.sh";
            File tmpFile = new File(tmpFilePath);
            OutputStreamWriter tmpWriter = null;

            if (!tmpFile.exists()) {
                try {
                    if (!tmpFile.createNewFile()) {
                        log("ERROR:创建文件失败," + tmpFilePath);
                        HeraLog.error("创建文件失败", tmpFile);
                    }
                    tmpWriter = new OutputStreamWriter(new FileOutputStream(tmpFile),
                            Charset.forName(jobContext.getProperties().getProperty("hera.fs.encode", "utf-8")));

                    tmpWriter.write("source " + shellFilePath);
                } catch (Exception e) {
                    jobContext.getHeraJobHistory().getLog().appendHeraException(e);
                } finally {
                    if (tmpWriter != null) {
                        try {
                            tmpWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            list.add(CommandUtils.changeFileAuthority(jobContext.getWorkDir()));
            list.add(CommandUtils.getRunShCommand(shellPrefix, tmpFilePath));
        } else {
            list.add("sh " + shellFilePath);
        }
        TaskLog.info("5.1 命令：{}", JSONObject.toJSONString(list));
        return list;
    }

    @Override
    public int run() throws Exception {
        return super.run();
    }

}
