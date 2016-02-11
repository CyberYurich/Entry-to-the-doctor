/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.presenter;

import com.exceptions.WrongDayOfWeekException;
import com.messageServise.interfaces.IMessageService;
import com.model.Entry;
import com.model.interfaces.IDbModel;
import com.model.interfaces.IEntry;
import com.presenter.interfaces.IPresenter;
import com.view.interfaces.IAllEntriesView;
import com.view.interfaces.ICalendarView;
import com.view.interfaces.IConnectView;
import com.view.interfaces.IDateEntriesView;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 *
 * @author ALEX
 */
public class MainPresenter implements IPresenter {

    private static final int TABLE_ROW_COUNT = 6;
    private static final int ONE_MINUTE_IN_MILLISECS = 60000;

    private final IDbModel dbModel;
    private final ICalendarView calendarView;
    private final IDateEntriesView dateEntriesView;
    private final IAllEntriesView allEntriesView;
    private final IConnectView connectView;
    private final IMessageService messageService;

    public MainPresenter(IDbModel dbModel,
            ICalendarView calendarView,
            IDateEntriesView dateEntriesView,
            IAllEntriesView allEntriesView,
            IConnectView connectView,
            IMessageService messageService) {
        this.dbModel = dbModel;
        this.calendarView = calendarView;
        this.dateEntriesView = dateEntriesView;
        this.allEntriesView = allEntriesView;
        this.connectView = connectView;
        this.messageService = messageService;

        calendarView.setPresenter(this);
        dateEntriesView.setPresenter(this);
        allEntriesView.setPresenter(this);
        connectView.setPresenter(this);
    }

    @Override
    public void showDateEntries() {
        try {
            Date date = calendarView.getCheckedDate();

            List<IEntry> entriesList = makeEntriesListByDate(date);
            dateEntriesView.setDataTable(entriesList);
            dateEntriesView.setCurentDate(date);

            calendarView.closeView();
            dateEntriesView.showView();

        } catch (WrongDayOfWeekException ex) {
            messageService.showError(new StringBuilder(90)
                    .append("Выбран неправильный день недели.\n")
                    .append("Воскресенье - неприемный день.")
                    .toString(),
                    "Неправильный день недели");
        } catch (SQLException | ClassNotFoundException ex) {
            messageService.showError(new StringBuilder(70)
                    .append("Соединение с базой данных утеряно\n")
                    .append("или некорректная таблица с данными.")
                    .toString(),
                    "Ошибка подключения");

            calendarView.lockEntriesView();
        }
    }

    @Override
    public void showAllEntries() {
        try {
            List<IEntry> entriesList = dbModel.readAll();
            Collections.sort(entriesList);
            allEntriesView.setDataTable(entriesList);

            calendarView.closeView();
            allEntriesView.showView();

        } catch (SQLException | ClassNotFoundException ex) {
            messageService.showError(new StringBuilder(70)
                    .append("Соединение с базой данных утеряно\n")
                    .append("или некорректная таблица с данными.")
                    .toString(),
                    "Ошибка подключения");

            calendarView.lockEntriesView();
        }
    }

    @Override
    public void showCalendar() {
        dateEntriesView.closeView();
        allEntriesView.closeView();
        connectView.closeView();
        calendarView.showView();
    }

    @Override
    public void showConnect() {
        calendarView.closeView();
        connectView.showView();
    }

    @Override
    public void createConnection() {

        try {
            dbModel.setConnectionParameters(connectView.getConnectParameters());
            messageService.showInformation("Соединение с базой данных установлено.",
                    "Успешное подключение");
            calendarView.unlockEntriesView();
            showCalendar();
        } catch (SQLException ex) {
            messageService.showError("Невозможно подключиться к базе данных.",
                    "Ошибка подключения");
        }
    }

    private List<IEntry> makeEntriesListByDate(Date date)
            throws SQLException, ClassNotFoundException {

        List<IEntry> resultList = new ArrayList<>();
        List<IEntry> entriesList = dbModel.readByDate(date);
        List<Time> timesList = makeTimesForDataTable(date);

        for (Time time : timesList) {
            boolean wasFound = false;
            for (IEntry entry : entriesList) {
                if (!time.before(entry.getTime()) && !time.after(entry.getTime())) {
                    resultList.add(entry);
                    wasFound = true;
                    break;
                }
            }
            if (!wasFound) {
                IEntry entry = new Entry();
                entry.setTime(time);
                resultList.add(entry);
            }
        }

        return resultList;
    }

    private List<Time> makeTimesForDataTable(Date date) {

        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        Time timeForRows;

        if (dayOfWeek == Calendar.SATURDAY) {
            timeForRows = Time.valueOf("10:00:00");
        } else {
            timeForRows = Time.valueOf("17:00:00");
        }

        List<Time> timesList = new ArrayList<>();
        for (int i = 0; i < TABLE_ROW_COUNT; ++i) {
            timesList.add(timeForRows);

            long t = timeForRows.getTime();
            timeForRows = new Time(t + (30 * ONE_MINUTE_IN_MILLISECS));
        }

        return timesList;
    }
}
