package com.mizo0203.lilywhite.repo;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.mizo0203.lilywhite.push_task.ReminderTaskServlet;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/* package */ class PushQueueRepository {

  private static final Logger LOG = Logger.getLogger(PushQueueRepository.class.getName());
  private final Queue mQueue;

  PushQueueRepository() {
    mQueue = QueueFactory.getDefaultQueue();
  }

  @SuppressWarnings("EmptyMethod")
  public void destroy() {
    // NOP
  }

  /**
   * リマインダータスクを追加する
   *
   * @param source_id
   *     送信先のID。Webhookイベントオブジェクトで返される、userId、groupId、またはroomIdの値を使用します。LINEアプリに表示されるLINE
   *     IDは使用しないでください。
   * @param etaMillis Sets the approximate absolute time to execute. (i.e. etaMillis is comparable
   *     with {@link System#currentTimeMillis()}).
   * @param message メッセージのテキスト。最大文字数：2000
   * @return タスク名 - App Engine によってタスクに一意の名前が割り当てられます
   */
  protected String enqueueReminderTask(String source_id, long etaMillis, String message) {
    LOG.info("enqueueReminderTask");
    return mQueue
        .add(
            TaskOptions.Builder.withUrl("/push_task/reminder_task")
                .param(ReminderTaskServlet.PARAM_NAME_SOURCE_ID, source_id)
                .param(ReminderTaskServlet.PARAM_NAME_MESSAGE, message)
                .etaMillis(etaMillis))
        .getName();
  }

  public void deleteReminderTask(@Nonnull String taskName) {
    // Delete an individual task...
    mQueue.deleteTask(taskName);
  }
}
