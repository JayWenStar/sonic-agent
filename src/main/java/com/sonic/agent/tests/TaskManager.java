package com.sonic.agent.tests;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.sonic.agent.tests.android.AndroidTestTaskBootThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Android测试任务线程管理
 *
 * @author Eason(master) & JayWenStar(slave)
 * @date 2021/11/25 10:53 上午
 */
@Slf4j
public class TaskManager {

    /**
     * key是boot的线程名，value是boot线程本身
     */
    private static ConcurrentHashMap<String, Thread> bootThreadsMap = new ConcurrentHashMap<>();


    /**
     * key是boot的线程名，value是boot线程启动的线程，因为是守护线程，所以当boot被停止后，child线程也会停止
     */
    private static ConcurrentHashMap<String, ConcurrentHashSet<Thread>> childThreadsMap = new ConcurrentHashMap<>();

    /**
     * 启动boot线程
     *
     * @param bootThread boot线程
     */
    public static void startBootThread(Thread bootThread) {
        bootThread.start();
        addBootThread(bootThread.getName(), bootThread);
    }

    /**
     * 启动boot线程（批量）
     *
     * @param bootThreads boot线程
     */
    public static void startBootThread(Thread...bootThreads) {
        for (Thread bootThread : bootThreads) {
            startBootThread(bootThread);
        }
    }

    /**
     * 启动子线程
     */
    public static void startChildThread(String key, Thread childThread) {
        childThread.start();
        addChildThread(key, childThread);
    }

    /**
     * 启动子线程（批量）
     */
    public static void startChildThread(String key, Thread...childThreads) {
        for (Thread childThread : childThreads) {
            startChildThread(key, childThread);
        }
    }


    /**
     * 添加boot线程
     *
     * @param key         用boot线程名作为key
     * @param bootThread  boot线程
     */
    public static void addBootThread(String key, Thread bootThread) {
        clearTerminatedThread();
        bootThreadsMap.put(key, bootThread);
    }


    /**
     * 添加child线程（单个）
     *
     * @param key         用boot的线程名作为key
     * @param childThread 线程
     */
    public static void addChildThread(String key, Thread childThread) {
        clearTerminatedThread();
        if (childThreadsMap.containsKey(key)) {
            ConcurrentHashSet<Thread> threadsSet = childThreadsMap.get(key);
            if (CollectionUtils.isEmpty(threadsSet)) {
                threadsSet = new ConcurrentHashSet<>();
                threadsSet.add(childThread);
                childThreadsMap.put(key, threadsSet);
                return;
            }
            threadsSet.add(childThread);
            return;
        }
        ConcurrentHashSet<Thread> threadsSet = new ConcurrentHashSet<>();
        threadsSet.add(childThread);
        childThreadsMap.put(key, threadsSet);
    }

    /**
     * 添加child线程（批量）
     *
     * @param key 用boot线程名作为key
     * @param set 线程set
     */
    public static void addChildThreadBatch(String key, ConcurrentHashSet<Thread> set) {
        clearTerminatedThread();
        if (childThreadsMap.containsKey(key)) {
            ConcurrentHashSet<Thread> threadsSet = childThreadsMap.get(key);
            if (CollectionUtils.isEmpty(threadsSet)) {
                childThreadsMap.put(key, set);
                return;
            }
            childThreadsMap.get(key).addAll(set);
            return;
        }
        childThreadsMap.put(key, set);
    }

    /**
     * 清除已经结束的线程，如果boot线程已经结束，若对应child线程未结束，则强制停止child线程
     */
    public static void clearTerminatedThread() {
        // 过滤出已经结束的boot线程组
        Map<String, Thread> terminatedThread = bootThreadsMap.entrySet().stream()
                .filter(t -> !t.getValue().isAlive())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        // 停止并删除boot线程
        terminatedThread.forEach((k, v) -> {
            v.interrupt();
            bootThreadsMap.remove(k);
        });
        // 删除boot衍生的线程
        terminatedThread.forEach((key, value) -> {
            childThreadsMap.remove(key);
        });
    }

    /**
     * 根据 result_id、caseId、udId停止任务
     */
    public static void forceStopSuite(int resultId, int caseId, String udid) {
        String key = String.format(AndroidTestTaskBootThread.ANDROID_TEST_TASK_BOOT_PRE, resultId, caseId, udid);
        // 停止boot线程
        Thread bootThread = bootThreadsMap.get(key);
        if (bootThread != null) {
            bootThread.interrupt();
        }
        // 清理map
        bootThreadsMap.remove(key);
        childThreadsMap.remove(key);
    }

}
