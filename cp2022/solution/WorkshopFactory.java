/*
 * University of Warsaw
 * Concurrent Programming Course 2022/2023
 * Java Assignment
 *
 * Author: Konrad Iwanicki (iwanicki@mimuw.edu.pl)
 * Author of assignment solution: Maria Wysogląd
 */
package cp2022.solution;

import java.util.Collection;
import java.util.LinkedList;

import cp2022.base.Workplace;
import cp2022.base.Workshop;


public final class WorkshopFactory {

    public final static Workshop newWorkshop(
            Collection<Workplace> workplaces
    ) {
       //throw ("not i");
       MyWorkshop new_workshop = new MyWorkshop();
        Collection<MyWorkplace> myworkplaces = new LinkedList<>();

        int maximalCapacity = 2 * workplaces.size();

        for (Workplace place: workplaces) {
            MyWorkplace new_my = new MyWorkplace(place.getId(), place, maximalCapacity, new_workshop);
            myworkplaces.add(new_my);
        }

        new_workshop.SetWorkshop(myworkplaces);


        return new_workshop;
    }
    
}
