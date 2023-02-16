package cp2022.solution;

import java.util.Collection;
import java.util.concurrent.*;
import javax.lang.model.element.Element;
import javax.swing.text.MutableAttributeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.Locale.IsoCountryCode;
import java.util.Set;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

public class MyWorkshop implements Workshop {
  private WorkplaceId nothing;
  private Collection < MyWorkplace > workplaces;
  private int capacityOfRegistration;
  private ConcurrentHashMap < WorkplaceId, MyWorkplace > mapOfWorkplaces;

  public ConcurrentHashMap < Long, WorkplaceId > threadsInWorkplaces;

  public ConcurrentHashMap < Long, Semaphore > threadsBlocked;
  public Semaphore MUTEX = new Semaphore(1);
  private ConcurrentHashMap < WorkplaceId, Semaphore > canWork;

  private BlockingQueue < Long > enteredThreads;
  private BlockingQueue < Long > waitingForEnterThreads;
  private int registrationLimit;
  private volatile boolean registrationBlocked = false;
  private int registrationEmptySpace = 0;

  private int enteredWorkers = 0;

  private boolean loop = false;

  public MyWorkshop() {}

  public void SetWorkshop(Collection < MyWorkplace > workplaces) {
    this.workplaces = workplaces;
    this.capacityOfRegistration = 2 * workplaces.size();
    this.threadsInWorkplaces = new ConcurrentHashMap < > ();
    this.threadsBlocked = new ConcurrentHashMap < > ();
    this.enteredThreads = new ArrayBlockingQueue < Long > (capacityOfRegistration);
    this.waitingForEnterThreads = new ArrayBlockingQueue < Long > (256);
    this.registrationLimit = capacityOfRegistration;

    this.mapOfWorkplaces = new ConcurrentHashMap < > ();
    for (MyWorkplace x: workplaces) {
      WorkplaceId id = x.getId();
      mapOfWorkplaces.put(id, x);
    }
  }

  public void MUTEXacquire() {
    try {
      MUTEX.acquire();
    } catch (Exception e) {

    }
  }

  public int LOOPcheck(MyWorkplace current, MyWorkplace new_workplace) {//} throws InterruptedException {
    if (current.getId().equals(new_workplace.getId())) {
      return 0;
    }

    int how_many = 1;

    while (!current.getId().equals(new_workplace.getId())) {
      if (new_workplace.wantSwitchTo == null) {
        return 0;
      } else {
        new_workplace = new_workplace.wantSwitchTo;
      }
      how_many++;
    }

    return how_many;
  }

  public void LOOPwakeUp(MyWorkplace current, MyWorkplace new_workplace, int how_many) {

    long previousThread = current.presentThread;
    current.loop = how_many;
    current.is_loop = true;

    current.previousThread = current.presentThread;
    long prev_newThread = new_workplace.presentThread;
    MyWorkplace copy_current = current;

    current.PREVIOUS = current.presentThread;

    current.should_be_unblocked = true;
    MyWorkplace copy_new = new_workplace;
    while (!current.getId().equals(new_workplace.getId())) {
      new_workplace.PREVIOUS = new_workplace.presentThread;
      new_workplace.should_be_unblocked = true;
      new_workplace.prev_workplace = copy_current;
      new_workplace.waiting = false;
      new_workplace.loop = how_many;
      new_workplace.is_loop = true;
      new_workplace.previousThread = new_workplace.presentThread;

      long newThreadId = new_workplace.presentThread;

      copy_current = new_workplace;
      previousThread = prev_newThread;
      new_workplace = new_workplace.wantSwitchTo;
      prev_newThread = new_workplace.presentThread;
    }

    current.prev_workplace = copy_current;
    current.queueSwitch.remove(current.presentThread);
  }

  public Workplace switchTo(WorkplaceId wid) {
    try {
      long myThread = Thread.currentThread().getId();

      MyWorkplace answer = mapOfWorkplaces.get(wid);

      MUTEXacquire();

      WorkplaceId oldWorkplaceId = threadsInWorkplaces.get(myThread);
      MyWorkplace oldWorkplace = mapOfWorkplaces.get(oldWorkplaceId);

      oldWorkplace.wantSwitchToId = answer.getId();
      oldWorkplace.wantSwitchTo = answer;


      int isLOOP = LOOPcheck(oldWorkplace, answer);

      if (!(isLOOP == 0)) {
        LOOPwakeUp(oldWorkplace, answer, isLOOP);
      } else {
        if (answer.presentThread == myThread) {
          answer.how_many_people--;
        } else if (answer.how_many_people > 0) {
          answer.queueSwitch.add(myThread);

          try {
            MUTEX.release();
            threadsBlocked.get(myThread).acquire();
          } catch (Exception e) {

          }
        }

      }

      answer.oldWorkplace = oldWorkplace;
      answer.presentThread = myThread;
      oldWorkplace.wantSwitchTo = null;
      oldWorkplace.wantSwitchToId = null;

      answer.how_many_people++;

      return answer;
    } catch (Exception e) {
      throw new RuntimeException("panic: unexpected thread interruption");
    }
  }

  public void registrationEnterNew() {
    long myThread = Thread.currentThread().getId();

    try {
      MUTEX.acquire();
    } catch (Exception e) {}

    if (registrationLimit > 0) {
      waitingForEnterThreads.remove(myThread);
      enteredThreads.add(Thread.currentThread().getId());
      registrationLimit--;
      enteredWorkers++;
      MUTEX.release();
    } else {
      MUTEX.release();
      try {
        threadsBlocked.get(myThread).acquire();
      } catch (Exception e) {

      }
    }
  }

  public Workplace enter(WorkplaceId wid) {
    waitingForEnterThreads.add(Thread.currentThread().getId());

    long myThread = Thread.currentThread().getId();
    if (!threadsBlocked.containsKey(myThread)) {
      Semaphore temp = new Semaphore(1);
      threadsBlocked.put(myThread, temp);
    }

    try {
      threadsBlocked.get(myThread).acquire();
    } catch (Exception e) {

    }

    registrationEnterNew();

    MyWorkplace answer = mapOfWorkplaces.get(wid);

    GETmyWorkplace(answer);

    return answer;
  }

  public void GETmyWorkplace(MyWorkplace answer) {
    MUTEXacquire();
    long myThread = Thread.currentThread().getId();

    if (answer.presentThread == myThread) {
      answer.how_many_people--;

    } else if (answer.how_many_people > 0) {
      answer.queueToWorkplace.add(myThread);

      try {
        MUTEX.release();
        threadsBlocked.get(myThread).acquire();
      } catch (Exception e) {

      }
    }

    answer.how_many_people++;
  }

  public void freeWorkplace(MyWorkplace releasedW) {//} throws InterruptedException {
    if (releasedW.how_many_people == 1) {
      releasedW.presentThread = -1;
    }

    long thisThreadId = Thread.currentThread().getId();
    releasedW.how_many_people--;
  }

  public void changeRegistrationStatusNew() {
    while (registrationLimit > 0 && !waitingForEnterThreads.isEmpty()) {
      enteredWorkers++;
      long currentElement = waitingForEnterThreads.poll();
      enteredThreads.add(currentElement);
      registrationLimit--;
      threadsBlocked.get(currentElement).release();
    }
  }

  public void registrationLeaveNew() {
    long myThread = Thread.currentThread().getId();
    enteredThreads.remove(myThread);
    enteredWorkers--;
    if (enteredWorkers == 0) {
      registrationLimit = capacityOfRegistration;
      changeRegistrationStatusNew();
    }
  }

  public void leave() {

    try {
      MUTEX.acquire();

      registrationLeaveNew();

      long thisThreadId = Thread.currentThread().getId();

      WorkplaceId released = threadsInWorkplaces.get(thisThreadId);
      MyWorkplace releasedW = mapOfWorkplaces.get(released);

      freeWorkplace(releasedW);

      int howManyPeopleCopy = releasedW.how_many_people;
      threadsBlocked.get(thisThreadId).release();
      releasedW.oldWorkplace = null;
      releasedW.isPriorityWorkplace = false;
      releasedW.prioritMyWorkplace = null;
      releasedW.oldWorkplace = null;
      releasedW.option = 0;
      releasedW.checked = false;
      releasedW.waiting = false;
      releasedW.wantSwitchTo = null;
      releasedW.wantSwitchToId = null;
      releasedW.previousThread = -1;
      releasedW.presentThread = -1;
      releasedW.loop = 0;
      releasedW.is_loop = false;
      releasedW.prev_workplace = null;
      releasedW.how_many_people = 0;
      releasedW.is_after_Semaphore = false;
      releasedW.should_be_unblocked = false;
      releasedW.PREVIOUS = -1;

      if (howManyPeopleCopy == 0) {

        if (!releasedW.queueSwitch.isEmpty()) {
          long newThread = releasedW.queueSwitch.poll();
          threadsBlocked.get(newThread).release();
        } else if (!releasedW.queueToWorkplace.isEmpty()) {
          long newThread = releasedW.queueToWorkplace.poll();
          threadsBlocked.get(newThread).release();
        } else {
          MUTEX.release();
        }

      } else {
        MUTEX.release();
      }

    } catch (Exception e) {
    }
  }
}