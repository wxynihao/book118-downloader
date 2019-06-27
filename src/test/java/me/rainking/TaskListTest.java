package me.rainking;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TaskListTest {

    DocumentBrowser pBrowser = new DocumentBrowser();

    @Test
    public void readTaskList() {
        List<String> pLists = pBrowser.readTaskList();
        pLists.forEach(item -> System.out.println(item));
    }

    @Test
    public void writeTaskList() {
        List<String> pList = new ArrayList<>();
        pList.add("7101132125001164");
        pList.add("5134312044001323");

        pBrowser.writeTaskList(pList);
    }
}
