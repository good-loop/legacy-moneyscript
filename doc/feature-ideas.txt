
# features to add

List of values, with optional check-dates
> Sales: 1,3,2,4,2,0(December),

timing & hence debts & accruals
Sales: £200   
SalesIncome: Sales 2 months ago

grouping of filters, e.g.
> at month 1:
> 	A: ...

nested groups

import of other sheets, and spreadsheets		

vat?! A: £10 + vat or + tax@17.5%...
which evaluates to £11.75 but also creates a vat row??
CashAtBank each quarter: -sum(VAT from previous quarter)

multiple output sheets, e.g. annual balance sheet, P&L, cashflow

sections within a sheet

"default" code

transfer, e.g.  
> Profit each December: transfer 10% to StaffBonuses

## gantt

Task1 for 2 weeks: 1 person
Task2 from week 1 for 4 weeks: 1 person
Task3 after Task1, Task2 for 2 weeks: 1 person
Task4 for 1 month: using Daniel
Task5 for 1 month: using Daniel // this should not be able to start until Task4 ends
Daniel: 100%
all: plot gantt

## scenarios

Scenarios are a bit like distributions: they lead to multiple runs.
Except: the results do not get merged. They're plotted / displayed separately
E.g. the rules

scenario FixedRate:
	Interest: 5%
scenario Tracker:
	Base: 4% +- 1% 
	Interest: Base + 2%

would lead to two tables & two charts
all scenarios are mutually exclusive??	
