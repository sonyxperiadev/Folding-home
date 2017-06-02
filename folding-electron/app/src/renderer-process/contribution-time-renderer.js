/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

const electron = require('electron');
const {ipcRenderer} = electron;
const Highcharts = require('highcharts');
const moment = require('moment');
require("moment-duration-format");


let contributionTimeJson;
let indexMonth = 0; // index that select the month to be shown

const FEBRUARY = "February";
const CHART_7_DAYS = "CHART_7_DAYS";
const CHART_BY_MONTH = "CHART_BY_MONTH";
const CHART_ALL_YEARS = "CHART_ALL_YEARS";
const CHART_REFRESH_INTERVAL = 300000;
const contributionMenuItem = document.getElementById('contribution-menu-item');
const chart7DaysButton = document.getElementById('chart7Days');
const chartByMonthButton = document.getElementById('chartByMonth');
const chartAllYearButton = document.getElementById('chartAllYear');
const previousMonthButton = document.getElementById("clickButtonPrevious");
const laterMonthButton = document.getElementById("clickButtonLater");
const nothingContributed = document.getElementById("nothingContributed");
const textNothingContributed = document.getElementById("textNothingContributed");
let containerChart = document.getElementById("container");
let interval;
let whichChart = CHART_7_DAYS;

/*Function to draw the chart Last 7 days*/
function drawChartLast7Days(contributionTimeJson) {
    let dayTimes = [];
    let weekDays = [];
    let sumWeekTimes = 0;

    $(function () {

        prepareDateToChart(contributionTimeJson);

        let sumWeekTimesConvertedToFloat = parseFloat(moment.duration(sumWeekTimes, "ms").format({
            template: "h",
            precision: 2
        }));

        Highcharts.chart('container', {
            chart: {
                type: 'area'
            },
            credits: {
                enabled: false
            },
            title: {
                text: 'Contribution Time - The Last 7 Days',
                x: -20 //center
            },
            subtitle: {
                text: sumWeekTimesConvertedToFloat + ' hours contributed',
                x: -20
            },
            xAxis: {
                categories: weekDays
            },
            yAxis: {
                title: {
                    text: 'Time'
                },
                min: 0, max: 24,
                plotLines: [{
                    value: 0,
                    width: 1,
                    color: '#808080'
                }]
            },
            tooltip: {
                min: 0,
                valueSuffix: 'h'
            },
            legend: {
                layout: 'vertical',
                align: 'right',
                verticalAlign: 'middle',
                borderWidth: 0
            },
            series: [{
                name: 'Hours',
                data: dayTimes
            }]
        });
    });

    /*Function to prepare arrays with contribution time of day and day name (DDD)*/
    function prepareDateToChart(contributionTimeJson) {
        if (contributionTimeJson.length > 6) {
            for (let i = 6; i >= 0; i--) {
                populateArrayToChart(i);
            }
        } else {
            for (let i = contributionTimeJson.length - 1; i >= 0; i--) {
                populateArrayToChart(i);
            }
        }
    }

    /*Function that populate the arrays to chart that are used into the function prepareDateToChart*/
    function populateArrayToChart(index) {
        let contributedTime = contributionTimeJson[index].contributed_time;
        sumWeekTimes += contributedTime;
        let contributedTimeConvertedToFloat = parseFloat(moment.duration(contributedTime, "ms").format({
            template: "h",
            precision: 2
        }));
        let dayDate = moment(contributionTimeJson[index].date).format('ddd');
        dayTimes.push([contributedTimeConvertedToFloat]);
        weekDays.push([dayDate]);
    }
}

/*Function to draw the chart By Month*/
function drawChartByMonth(contributionTimeJson) {
    let sumMonthTimes = 0;

    let objByMonthYear = dataOrganizedByMonthYear(contributionTimeJson);

    let keysFromObjByMonthYear = Object.keys(objByMonthYear);
    let olderMonth = keysFromObjByMonthYear[keysFromObjByMonthYear.length - 1];

    let givenMonth = moment().subtract(indexMonth, "month").format("YYYY-MM");
    let givenMonthName = moment().subtract(indexMonth, "month").format("MMMM");
    let givenMonthFormatToChart = moment().subtract(indexMonth, "month").format("MMMM YYYY");

    let objCurrentMonth = objByMonthYear[givenMonth];

    if (objCurrentMonth !== undefined) {
        containerChart.style.display = 'block';
        nothingContributed.style.display = 'none';
        textNothingContributed.style.display = 'none';
        $(function () {
            objCurrentMonth.forEach(function (item, index) {
                item.origOrder = index;
            });
            let currentMonthDays = objCurrentMonth.sort(sortArrayByDateDESC);
            let currentMonthDaysReversed = currentMonthDays.reverse();

            let week1, week2, week3, week4, week5;

            let week1Days = [];
            let week1Times = [];
            let week2Days = [];
            let week2Times = [];
            let week3Days = [];
            let week3Times = [];
            let week4Days = [];
            let week4Times = [];
            let week5Days = [];
            let week5Times = [];

            /* To fix when the month that started to contribute begins with date different of first day*/
            if (olderMonth == givenMonth) {
                completeMissingDays(currentMonthDaysReversed);
            }

            week1 = currentMonthDaysReversed.slice(0, 7);
            week2 = currentMonthDaysReversed.slice(7, 14);
            week3 = currentMonthDaysReversed.slice(14, 21);
            week4 = currentMonthDaysReversed.slice(21, 28);

            populateArraysWeekDaysAndWeekTimes(week1, week1Times, week1Days);
            populateArraysWeekDaysAndWeekTimes(week2, week2Times, week2Days);
            populateArraysWeekDaysAndWeekTimes(week3, week3Times, week3Days);
            populateArraysWeekDaysAndWeekTimes(week4, week4Times, week4Days);

            let sumMonthTimesToChart = parseFloat(moment.duration(sumMonthTimes, "ms").format({
                template: "h",
                precision: 2
            }));

            let weeksArray = [week1Days, week2Days, week3Days, week4Days];

            let daysToXAxisChart = week1Days;
            for (let i = 1; i <= weeksArray.length - 1; i++) {
                if (daysToXAxisChart.length < weeksArray[i].length) {
                    daysToXAxisChart = weeksArray[i];
                }
            }

            var chart = Highcharts.chart('container', {
                chart: {
                    type: 'area',
                },
                credits: {
                    enabled: false
                },
                title: {
                    text: 'Contribution Time - ' + givenMonthFormatToChart,
                    x: -20 //center
                },
                subtitle: {
                    text: sumMonthTimesToChart + ' hours contributed',
                    x: -20
                },
                xAxis: {
                    categories: daysToXAxisChart,
                    min: 0,
                },
                yAxis: {
                    title: {
                        text: 'Time'
                    },
                    min: 0, max: 24,
                    plotLines: [{
                        value: 0,
                        width: 1,
                        color: '#808080'
                    }]
                },
                tooltip: {
                    valueSuffix: 'h'
                },
                legend: {
                    layout: 'vertical',
                    align: 'right',
                    verticalAlign: 'middle',
                    borderWidth: 0
                },
                series: [{
                    name: 'Week 1',
                    data: week1Times
                }, {
                    name: 'Week 2',
                    data: week2Times
                }, {
                    name: 'Week 3',
                    data: week3Times
                }, {
                    name: 'Week 4',
                    data: week4Times
                }]
            });

            if (givenMonthName != FEBRUARY){
                week5 = currentMonthDaysReversed.slice(28, 31);
                populateArraysWeekDaysAndWeekTimes(week5, week5Times, week5Days);
                chart.addSeries({
                    name: 'Week 5',
                    data: week5Times
                });
            }
        });
    } else {
        mountInfoToNotContributionMonth(givenMonthFormatToChart);
    }

    /*The function that populate the arrays with the names of the day(with this format "DDD") and the contribution time of each day*/
    function populateArraysWeekDaysAndWeekTimes(weekX, weekXTimes, weekXDays) {
        for (let i = 0; i <= weekX.length - 1; i++) {
            let contributedTime = weekX[i].contributed_time;
            sumMonthTimes += contributedTime;
            let contributedTimeConvertedToFloat = parseFloat(moment.duration(contributedTime, "ms").format({
                template: "h",
                precision: 2
            }));
            weekXTimes.push([contributedTimeConvertedToFloat]);
            weekXDays.push(moment(weekX[i].date).format('ddd'));
        }
    }

    /*Function to sort the data by date*/
    function sortArrayByDateDESC(a, b) {
        let diff = moment(b.date).valueOf() - moment(a.date).valueOf();
        if (diff !== 0) {
            return diff;
        } else {
            return moment(b.origOrder).valueOf() - moment(a.origOrder).valueOf();
        }
    }

    /*Function to organize the data by Month Year(YYYY-MM)*/
    function dataOrganizedByMonthYear(contributionTimeJson) {
        let objByMonthYear = [];
        for (let i = 0; i < contributionTimeJson.length; i++) {
            if (objByMonthYear[contributionTimeJson[i].date.slice(0, 7)] === undefined) {
                objByMonthYear[contributionTimeJson[i].date.slice(0, 7)] = [];
            }
            objByMonthYear[contributionTimeJson[i].date.slice(0, 7)].push(contributionTimeJson[i]);
        }
        return objByMonthYear;
    }


    function completeMissingDays(currentMonthDaysReversed) {
        if (currentMonthDaysReversed.length > 1) {
            let startDate = moment(currentMonthDaysReversed[0].date);
            let firstDayMonth = moment(currentMonthDaysReversed[0].date.slice(0, 7) + "-01");

            let diffDays = Math.abs(firstDayMonth.diff(startDate, 'days') - 1);

            for (let i = 1; i < diffDays; i++) {
                let dateToFill = moment(startDate).subtract(i, 'days').format("YYYY-MM-DD");
                currentMonthDaysReversed.push({"date": dateToFill, "contributed_time": 0});
            }
            currentMonthDaysReversed.sort(sortArrayByDateDESC);
            currentMonthDaysReversed.reverse();
        }
    }
}

/*Function to draw the chart All Year*/
function drawChartAllYears(myDataJson) {

    let years = [];
    let sumTotalYears = 0;

    let objByYear = dataOrganizedByYear(myDataJson);
    let timeEachYear = sumTimeEachYear();

    let sumAllYearsTimesToChart = parseFloat(moment.duration(sumTotalYears, "ms").format({
        template: "h",
        precision: 2
    }));

    years.reverse();
    timeEachYear.reverse();

    $(function () {
        Highcharts.chart('container', {
            chart: {
                type: 'area'
            },
            credits: {
                enabled: false
            },
            title: {
                text: 'Contribution Time - All Years',
                x: -20 //center
            },
            subtitle: {
                text: sumAllYearsTimesToChart + ' hours contributed',
                x: -20
            },
            xAxis: {
                categories: years
            },
            yAxis: {
                title: {
                    text: 'Time'
                },
                min: 0,
                plotLines: [{
                    value: 0,
                    width: 1,
                    color: '#808080'
                }]
            },
            tooltip: {
                valueSuffix: 'h'
            },
            legend: {
                layout: 'vertical',
                align: 'right',
                verticalAlign: 'middle',
                borderWidth: 0
            },
            series: [{
                name: 'Hours',
                data: timeEachYear
            }]
        });
    });

    /*Function to organize the data by year */
    function dataOrganizedByYear(contributionTimeJson) {
        let objByYear = [];
        for (let i = 0; i < contributionTimeJson.length; i++) {
            let yearFormatYYYY = contributionTimeJson[i].date.slice(0, 4);
            if (objByYear[yearFormatYYYY] === undefined) {
                objByYear[yearFormatYYYY] = [];
            }
            if (($.inArray(yearFormatYYYY, years) == -1)) {
                years.push(yearFormatYYYY);
            }
            objByYear[yearFormatYYYY].push(contributionTimeJson[i]);
        }
        return objByYear;
    }

    /*Function to get sum of contribution time of each year*/
    function sumTimeEachYear() {
        let timeEachYear = [];
        for (let i = 0; i < years.length; i++) {
            let sumEachYear = 0;
            for (let j = 0; j < objByYear[years[i]].length; j++) {
                sumEachYear += objByYear[years[i]][j].contributed_time;
                sumTotalYears += objByYear[years[i]][j].contributed_time;
            }
            let contributedTimeConvertedToFloat = parseFloat(moment.duration(sumEachYear, "ms").format({
                template: "h",
                precision: 2
            }));
            timeEachYear.push(contributedTimeConvertedToFloat);
        }
        return timeEachYear;
    }
}

function setupElements() {
    previousMonthButton.style.display = 'none';
    laterMonthButton.style.display = 'none';
    containerChart.style.display = 'block';
    nothingContributed.style.display = 'none';
    textNothingContributed.style.display = 'none';
}

function mountInfoToNotContributionMonth(givenMonthFormatToChart) {
    textNothingContributed.textContent = "There was no contribution on " + givenMonthFormatToChart;
    textNothingContributed.style.display = 'block';
    containerChart.style.display = 'none';
    nothingContributed.style.display = 'block';
}

contributionMenuItem.addEventListener('click', function () {
    requestContributionTimeJson();
    whichChart = CHART_7_DAYS;
    clearInterval(interval);
    interval = setInterval(requestContributionTimeJson, CHART_REFRESH_INTERVAL);
    setupElements();
});

chart7DaysButton.addEventListener('click', function () {
    requestContributionTimeJson();
    whichChart = CHART_7_DAYS;
    setupElements();
});

chartByMonthButton.addEventListener('click', function () {
    requestContributionTimeJson();
    whichChart = CHART_BY_MONTH;
    previousMonthButton.style.display = 'block';
    laterMonthButton.style.display = 'block';

});

chartAllYearButton.addEventListener('click', function () {
    requestContributionTimeJson();
    whichChart = CHART_ALL_YEARS;
    setupElements();

});

previousMonthButton.addEventListener('click', function () {
    indexMonth += 1;
    drawChartByMonth(contributionTimeJson);
});

laterMonthButton.addEventListener('click', function () {
    indexMonth -= 1;
    drawChartByMonth(contributionTimeJson);
});

ipcRenderer.on(global.eventMessages.gotContributionTime, function (event, contributionTime) {
    contributionTimeJson = contributionTime;
    if (whichChart == CHART_7_DAYS) {
        drawChartLast7Days(contributionTimeJson);
    } else if (whichChart == CHART_BY_MONTH) {
        drawChartByMonth(contributionTimeJson);
    } else if (whichChart == CHART_ALL_YEARS) {
        drawChartAllYears(contributionTimeJson);
    }
});

function requestContributionTimeJson() {
    ipcRenderer.send(global.eventMessages.getContributionTime);
}

requestContributionTimeJson();


