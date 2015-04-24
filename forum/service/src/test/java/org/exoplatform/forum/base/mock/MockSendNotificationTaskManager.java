package org.exoplatform.forum.base.mock;

import java.util.concurrent.LinkedBlockingQueue;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.service.task.SendNotificationTaskManager;
import org.exoplatform.forum.service.task.AbstractForumTask.SendNotificationTask;

public class MockSendNotificationTaskManager extends SendNotificationTaskManager {

  public MockSendNotificationTaskManager(InitParams params) {
    super(params);
  }

  @Override
  public void addTask(SendNotificationTask task) {
    if (tasks == null) {
      tasks = new LinkedBlockingQueue<SendNotificationTask>();
    }
    tasks.add(task);
    //
    commit(true);
  }
}
