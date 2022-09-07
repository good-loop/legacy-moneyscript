Invest at month 1: £100k

Staff:
   Manager: £30k per year
   Shop assistant: £18k per year

Costs:
   Overheads: 10% * Staff
   Goods: £1k +- 250
   Rent: £10k per year

// A growing customer base
Customers: (10 +- 5) + month
Sales: Customers * (10 +- 5)

Income: Sales * £25
   
Cashflow: Income - Costs - Staff
Balance: sum(Invest) + sum(Cashflow)

// Hire more staff once business is good
Staff:
   Shop Assistant 2 from Income > £8k: £18k per year

// Output some graphs
Balance: plot
Income, Costs: plot

// some styling
Balance: {font-weight:bold}
Cashflow if Cashflow < 0: {color:red}
