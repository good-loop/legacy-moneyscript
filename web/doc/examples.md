
### Basic Example

This shows (1) how to create rows, and (2) how to use them in simple formulas.
The plan will run for 2 years by default. The formulas are calculated on a monthly basis.
	
	Buy: £9
	Sell: £10 // Sell for a bit more :) By the way, this bit after the // is a comment
	Customers: 100
	Income: Customers * (Sell - Buy)
	Balance: sum Income

### Groups of Rows

	Costs:
		Studio Rent: £10k per year
		Materials: £500 per week		
		Electricity: £100 per month	
			
	Sales: 500 +- 100 // an uncertain (random) amount
	Income:	£5 * Sales

	// When a group	is used in a formula, we add up the rows within that group
	Cashflow: Income - Costs


### Using Filters

	Sales: £100
	Costs at month 1: £100 // a filter in the Cell-Selection part of the rule
	Costs from month 3: £20 // this rule will replace the one above when it kicks in

	// We can group a few filtered rules under one filter
	Later from month 6:
		Sales: £200
		Costs: £50 

	Celebrate from Sales > £150: 1 // a conditional filter

### Replacing versus Stacking

	// Here, when the 2nd rule kicks in, it will completely replace the 1st rule.
	Alice: £20k per year
	Alice from year 2: £22k per year

	// Here, when the 2nd rule kicks in, it will be stacked after the 1st.
	// So from year 2, Alice's salary becomes £20k * 110%
	Alice: £20k per year
	Alice from year 2: * 110%

### Sum
In this example, Balance is the sum of Profit from start uptil the current point:

	Profit: £1 per month
	Balance: sum Profit
