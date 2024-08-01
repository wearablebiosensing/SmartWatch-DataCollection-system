package com.example.sensorsfilesave;

public class TargetClass implements ItemSelectionListener {

    @Override
    public void onItemSelected(String selectedItem) {
        // Handle the selected item string
        System.out.println("Selected item: " + selectedItem);
        // Perform any required action with the selected item
    }
}