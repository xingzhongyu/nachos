package nachos.threads;

import nachos.ag.BoatGrader;

import java.awt.*;

public class Boat {
    static BoatGrader bg;
    private static Lock lock;
    static Condition2 waitAdultsInO;
    static Condition2 waitChildrenInM;
    static Condition2 waitChildrenInO;
    static int adultsInO;
    static int childrenInM;
    static int childrenInO;
    static boolean adultGo;
    static boolean boatIn0;
    static boolean boatIsEmpty;
    static boolean allPeopleGo;

    public static void selfTest() {


        BoatGrader b = new BoatGrader();

//	System.out.println("\n ***Testing Boats with only 2 children***");
//        begin(2, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);



  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
  	begin(3, 3, b);

    }

    public static void begin(int adults, int children, BoatGrader b) {
        // Store the externally generated autograder in a class
        // variable to be accessible by children.

        bg = b;
        lock = new Lock();
        waitAdultsInO = new Condition2(lock);
        waitChildrenInM = new Condition2(lock);
        waitChildrenInO = new Condition2(lock);
        adultsInO = adults;
        childrenInM = 0;
        adultGo = false;
        boatIn0 = true;
        boatIsEmpty = true;
        childrenInO = children;
        allPeopleGo = false;

        for (int i = 0; i < adults; i++) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    AdultItinerary();
                }
            };
            KThread kThread = new KThread(r);
            kThread.fork();
        }
        for (int i = 0; i < children; i++) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ChildItinerary();
                }
            };
            KThread kThread = new KThread(r);
            kThread.fork();
        }
        // Instantiate global variables here

        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.

//	Runnable r = new Runnable() {
//	    public void run() {
//                SampleItinerary();
//            }
//        };
//        KThread t = new KThread(r);
//        t.setName("Sample Boat Thread");
//        t.fork();

    }

    static void AdultItinerary() {



        lock.acquire();

        if (!(adultGo && boatIn0)) {
            waitAdultsInO.sleep();//?????????????????????????????????????????????

        }


        adultsInO--;
        boatIn0 = false;
        adultGo = false;
        bg.AdultRowToMolokai();
        waitChildrenInM.wake();//?????????????????????????????????

        lock.release();
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
    }

    static void ChildItinerary() {
        lock.acquire();
        while (!allPeopleGo) {

            if (boatIn0) {
//                if (adultGo){
//                    waitAdultsInO.wake();
//
//                    waitChildrenInO.sleep();
//                }?????????????????????O?????????????????????0????????????????????????????????????????????????????????????

                if (boatIsEmpty) {
                    bg.ChildRowToMolokai();
                    childrenInO--;
                    childrenInM++;
                    boatIsEmpty = false;

                    waitChildrenInO.wake();
                } else {
                    bg.ChildRideToMolokai();
                    childrenInO--;
                    childrenInM++;
                    boatIsEmpty = true;
                    boatIn0 = false;
                    waitChildrenInM.wake();
                    if (childrenInO == 0 && adultsInO == 0) {//??????????????????????????????????????????????????????
                        allPeopleGo = true;
                    }
                    if (childrenInO == 0 && adultsInO != 0) {//?????????????????????????????????
                        adultGo = true;
                    }
                }
                waitChildrenInM.sleep();//??????????????????M??????
            } else {
                bg.ChildRowToOahu();
                childrenInM--;
                childrenInO++;
                boatIsEmpty = true;
                boatIn0 = true;
                if (adultGo) {
                    waitAdultsInO.wake();
                } else {

                    waitChildrenInO.wake();
                }
                waitChildrenInO.sleep();//  ????????????O??????
            }
        }
        lock.release();
    }

    static void SampleItinerary() {
        // Please note that this isn't a valid solution (you can't fit
        // all of them on the boat). Please also note that you may not
        // have a single thread calculate a solution and then just play
        // it back at the autograder -- you will be caught.
        System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();
    }

}
