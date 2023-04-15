// Implemented solution to Java Assignment
// Author: Maria Wysogląd
package cp2022.solution;

import java.util.Collection;
import java.util.concurrent.*;

import javax.swing.text.MutableAttributeSet;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

public class MyWorkplace extends Workplace {
  public Workplace original;
  public Semaphore semaphoreWorkplace = new Semaphore(1);
  public Semaphore semaphoreWork = new Semaphore(1);

  public BlockingQueue < Long > queueSwitch;
  public BlockingQueue < Long > queueToWorkplace;
  public boolean isPriorityWorkplace = false;
  public MyWorkplace prioritMyWorkplace = null;

  public MyWorkplace oldWorkplace = null;
  public int option = 0;
  public boolean checked = false;
  public MyWorkshop myWorkshop;
  public boolean waiting = false;
  public MyWorkplace wantSwitchTo;
  public WorkplaceId wantSwitchToId;
  public long previousThread = -1;
  public long presentThread = -1;
  public int loop = 0;
  public boolean is_loop = false;
  public MyWorkplace prev_workplace;
  public Semaphore loop_Semaphore = new Semaphore(1);
  public int how_many_people = 0;
  public boolean is_after_Semaphore = false;
  public boolean should_be_unblocked = false;
  public long PREVIOUS = -1;

  public MyWorkplace(WorkplaceId id, Workplace original, int maximalCapacity, MyWorkshop myWorkshop) {
    super(id);
    this.original = original;
    this.queueToWorkplace = new ArrayBlockingQueue < > (256);
    this.queueSwitch = new ArrayBlockingQueue < > (256);
    this.myWorkshop = myWorkshop;
  }

  public void notifyLoop() {
    long prev = previousThread;
    MyWorkplace prev_work = prev_workplace;
    this.loop--;
    while (!prev_work.getId().equals(this.getId())) {
      prev_work.loop--;
      prev_work = prev_work.prev_workplace;
    }
  }

  @Override
  public void use() {
    presentThread = Thread.currentThread().getId();

    myWorkshop.threadsInWorkplaces.put(presentThread, this.getId());

    if (is_loop) {
      oldWorkplace.how_many_people--;
    }

    if (is_loop) {
      this.queueSwitch.remove(presentThread);
    }

    myWorkshop.MUTEX.release();

    try {
      loop_Semaphore.acquire();

    } catch (Exception e) {

    }

    myWorkshop.MUTEXacquire();
    if (is_loop && should_be_unblocked == true) {
      oldWorkplace.should_be_unblocked = false;
      myWorkshop.threadsBlocked.get(this.PREVIOUS).release();
    } else {
      myWorkshop.MUTEX.release();
    }
    myWorkshop.MUTEXacquire();

    if (oldWorkplace != null && (is_loop == false) && !(oldWorkplace.getId().equals(this.getId()))) {
      oldWorkplace.waiting = false;
      myWorkshop.freeWorkplace(oldWorkplace);

      int howManyPeopleCopy = oldWorkplace.how_many_people;

      long threadSwitch = -1;
      long threadEnter = -1;

      boolean queueSwitch1Empty = oldWorkplace.queueSwitch.isEmpty();
      boolean queueEnterEmpty = oldWorkplace.queueToWorkplace.isEmpty();

      if (howManyPeopleCopy == 0) {
        if (!queueSwitch1Empty) {
          threadSwitch = oldWorkplace.queueSwitch.poll();
        } else if (!queueEnterEmpty) {
          threadEnter = oldWorkplace.queueToWorkplace.poll();
        }
      }

      oldWorkplace = null;
      presentThread = Thread.currentThread().getId();

      if (howManyPeopleCopy == 0) {
        if (!queueSwitch1Empty) {
          myWorkshop.threadsBlocked.get(threadSwitch).release();

        } else if (!queueEnterEmpty) {
          myWorkshop.threadsBlocked.get(threadEnter).release();
        } else {
          myWorkshop.MUTEX.release();
        }
      } else {
        myWorkshop.MUTEX.release();
      }
    } else if (is_loop == true) {
      presentThread = Thread.currentThread().getId();
      notifyLoop();

      if (loop == 0 && is_loop == true) {
        long prev = previousThread;
        MyWorkplace prev_work = prev_workplace;

        while (!prev_work.equals(this)) {
          prev_work.loop_Semaphore.release();
          prev_work = prev_work.prev_workplace;
        }

        myWorkshop.MUTEX.release();
      } else {
        try {
          myWorkshop.MUTEX.release();
          loop_Semaphore.acquire();
        } catch (Exception e) {

        }
      }

    } else {
      myWorkshop.MUTEX.release();
    }

    original.use();
    this.PREVIOUS = -1;
    this.should_be_unblocked = false;

    myWorkshop.MUTEXacquire();
    is_loop = false;

    loop_Semaphore.release();
    is_after_Semaphore = false;
    myWorkshop.MUTEX.release();
  }

}
