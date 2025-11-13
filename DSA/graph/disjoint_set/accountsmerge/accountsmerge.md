```

Input : 
[
 ["John","johnsmith@mail.com","john_newyork@mail.com"],
 ["John","johnsmith@mail.com","john00@mail.com"],
 ["Mary","mary@mail.com"],
 ["John","johnnybravo@mail.com"]
]


Step 1 : Process each account and union emails in the same account
- Take 1st email -> make it parent of itself if not already
- For each subsequent email in the account, union it with the 1st email
- This way all emails in the same account are connected

1.  Make each email its own parent initially

johnsmith@mail.com → johnsmith@mail.com
john_newyork@mail.com → john_newyork@mail.com

2. union(johnsmith@mail, john_newyork@mail)
- attach new york under john smith

johnsmith@mail.com → johnsmith@mail.com
john_newyork@mail.com → johnsmith@mail.com


2nd account 
john00@mail.com → john00@mail.com

```