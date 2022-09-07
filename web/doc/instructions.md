
# MoneyScript Manual

MoneyScript is a *structured format* for business plans.
It's easy to create, and easy to understand -- yet it sets out a business
plan as precisely as any spreadsheet (and with less room for mistakes).

<div id='faq'></div>

## The Process of Using MoneyScript

1. Write a MoneyScript plan in the MoneyScript Editor.
2. The app reads that plan. If the format is wrong, don't panic, you'll get an error to let you know where the problem is.
3. The app runs the plan to generate a spreadsheet.
4. You get back: the spreadsheet and the charts you've requested.

## Basic Format: Each line is a row

A MoneyScript describes a spreadsheet. Each line of text is a row. 
The columns are months, and the spreadsheet covers 2 years.
For example:

	Rent: £12,000 per year

This will create a row called `Rent`, with monthly entries that add up to £12,000 per year.

A row consists of a *Cell-Selection* (such as a row name) and a *Formula*. The format is:

	Cell-Selection: Formula

Notice the ":" colon between the Cell-Selection and the Formula!
In the example above, `Rent` selects the row called Rent - and it also creates that row.
`£12,000 per year` is a simple formula which amounts to £1,000 per month.

## Row names and Cell-Selections

### Row Names

You can have rows for costs, income, number of customers, the temperature -- anything you like.
There are a few rules about row names:

1. Row names must start with a capital letter. E.g. `Rent` is good, but `rent` won't work. 
This is because MoneyScript keywords are always lowercase.
2. Row names can have several words, e.g. `Edinburgh Office` is OK.
3. Row names cannot include: 
	- Punctuation.
	- Numbers. `Waiter no 2` is bad. Though you can have numbers-as-part-of-words, so `Waiter no2` is OK.
	- MoneyScript keywords, such as `from`. There's a [list of keywords](#keywords) later.
4. Row names are case sensitive. So if you create a row `Rent`, you can't later refer to it
as `RENT`.

These rules help avoid ambiguity, and hence avoid mistakes.
Don't worry if you forget them - you'll get a helpful error message.

### Cell-Selections

Behind the scenes we're creating a spreadsheet - and a spreadsheet is made up of table cells.
A *cell-selection* is a snippet of text which picks out a set of cells.

The simplest cell-selection is just a row name, which selects that row.

More complex cell-selections can use particular periods or conditions. Here are some examples:

	Rent from year 2
	Interest if Balance < 0
	Equipment at month 1
	Alice, Bob	// This selects two rows, Alice and Bob	

### Time based filters

from
to
at


### Conditional filters

Suppose our shop plans to hire someone once they have enough custom. This rule uses a condition to kick in once there are over 100 Customers:

	SecondFishMonger from Customers > 100: £20k per year

The condition is `Customers > 100`. The `from` keyword means the rule kicks in once condition is reached, and then stays on afterwards. Suppose we have a rule which switches on and off - then we use the `if` keyword, plus a condition:

	StaffParty if Sales > 20: £1k

You can even use formulas inside a condition. With more complex expressions, it's a good idea to start using brackets. For example:

	StaffParty if ((Income - Costs) > £10k): £1k

## Formulas

Formulas let you say how rows relate to each other. 

 - You can use row names in a formula. For example, `Income - Costs`. The row name acts as the value for that row in the same column. There is a restriction: a formula should only refer to rows earlier in the spreadsheet.
 - You can use standard arithmetic: +, -, * (times), / (divide)
 - You can use brackets (and it's a good idea to do so - it makes it clear what goes with what). 
For example `sum(Sales) - £10`
 - You can use the functions `max`, `min`, `log` (log using base e), `x^y` (x to the power y), and `sum`.
For example `max(Balance,0)` or `MonthlyInterest^12`.

### Number formats

MoneyScript is flexible about how you express numbers. For example:

 - `1,000` = `1000` = `1k`
 - `1,000,000` = `1000000` = `1m`
 - `10%`

## Planning with Uncertainty

How much will you sell? You probably have some idea, but you can't give a precise figure.

The future is uncertain, and your plans should reflect that. By planning with uncertainty, you can see the risks and rewards, and be prepared for a range of outcomes. This is also known as sensitivity analysis, because
it shows you how sensitive your plan is to changes.
You can't really do this with a normal spreadsheet
but MoneyScript makes it easy. 

If you're unsure about a value, specify a range using `+-`. For example,
<code>100 +- 25</code> means "100 give or take 25", or to put it another way, "between 75 and 125".

The resulting spreadsheet and charts will show how that uncertainty affects other values.

*Advanced*: `+-` uses the Uniform Distribution. You can also use the Normal Distribution, aka the Gaussian Distribution, with the format <code>N(mean, variance)</code>. For example:

<code>Price of fish: N(£10, 16)</code>


## Changing the columns

You can alter how many columns are shown using the `columns` command. E.g. for a 6 month plan, do:

<code>columns: 6 months</code>

## Keyword Dictionary

- `£` or `$`	a filter used in cell-selections to pick out £-valued or $-valued cells and ignore other cells. `Cashflow: sum £ above`
- `above`	a filter used in cell-selections to pick out the cells above the current cell. E.g. `Cashflow: sum £ above`
- `at`		used in cell-selections to pick out a particular date or condition. E.g. `at month 3`
- `from`	used in cell-selections to filter from a particular date or condition. E.g. `from year 2`.
This includes the starting date.
- `previous`	used in formulas to pick the value from the previous month. E.g. `Stock: previous - Sales`
- `to`		opposite of `from`. Used in cell-selections to filter from a particular date or condition. E.g. `to year 2`. This includes the end date.
