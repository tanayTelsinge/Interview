package disjoint_set.accountsmerge;

import java.util.*;

/*
        Given a list of accounts where each element accounts[i] is a list of strings, 
        where the first element accounts[i][0] is a name, and the rest of the elements 
        are emails representing emails of the account.

        Now, we would like to merge these accounts. Two accounts definitely belong to the same person 
        if there is some common email to both accounts. Note that even if two accounts have the same name, 
        they may belong to different people as people could have the same name. A person can have any number 
        of accounts initially, but all of their accounts definitely have the same name.

        After merging the accounts, return the accounts in the following format: 
        the first element of each account is the name, and the rest of the elements are 
        emails in sorted order. The accounts themselves can be returned in any order.

 *  Example 1:

Input: accounts = [
    ["John","johnsmith@mail.com","john_newyork@mail.com"],
    ["John","johnsmith@mail.com","john00@mail.com"],["Mary","mary@mail.com"],
    ["John","johnnybravo@mail.com"]
]
Output: [
    ["John","john00@mail.com","john_newyork@mail.com","johnsmith@mail.com"],
    ["Mary","mary@mail.com"],
    ["John","johnnybravo@mail.com"]
]
Explanation:
The first and second John's are the same person as they have the common email "johnsmith@mail.com".
The third John and Mary are different people as none of their email addresses are used by other accounts.
We could return these lists in any order, for example the answer [
    ['Mary', 'mary@mail.com'], 
    ['John', 'johnnybravo@mail.com'], 
    ['John', 'john00@mail.com', 'john_newyork@mail.com', 'johnsmith@mail.com']
] would still be accepted.
*/
public class AccountsMerge {
    HashMap<String, String> emailToName = new HashMap<>();
    HashMap<String, String> parent = new HashMap<>();

    public String find(String s) {
        if (!parent.get(s).equals(s)) {
            parent.put(s, find(parent.get(s)));
        }
        return parent.get(s);
    }

    public void union(String x, String y) {
        String parentX = find(x);
        String parentY = find(y);
        if (!parentX.equals(parentY)) {
            parent.put(parentX, parentY);
        }
    }

    public List<List<String>> accountsMerge(List<List<String>> accounts) {
        for (List<String> account : accounts) {
            String name = account.get(0);
            for (int i = 1; i < account.size(); i++) {
                String email = account.get(i);
                emailToName.put(email, name);
                parent.putIfAbsent(email, email);
                union(account.get(1), email); // Union all emails in the same account
            }
        }

        // what above step does is that it connects all emails in the same account
        // eg. for account ["John","johnsmith@mail.com","john_newyork@mail.com"]
        // it will make parent relationships:
        // johnsmith@mail.com -> johnsmith@mail.com (initial)
        // john_newyork@mail.com -> johnsmith@mail.com (after union)
        // And for ["John","johnsmith@mail.com","john00@mail.com"]
        // john00@mail.com will also point to johnsmith@mail.com
        // This way all emails belonging to the same person are connected through the same parent

        HashMap<String, List<String>> unions = new HashMap<>();
        for (String email : parent.keySet()) {
            String rootEmail = find(email);
            unions.putIfAbsent(rootEmail, new ArrayList<>());
            unions.get(rootEmail).add(email);
        }

        // This step groups all emails by their root parent email
        // eg. unions will contain:
        // "johnsmith@mail.com" -> ["johnsmith@mail.com", "john_newyork@mail.com", "john00@mail.com"]
        // "mary@mail.com" -> ["mary@mail.com"]
        // "johnnybravo@mail.com" -> ["johnnybravo@mail.com"]
        // Each key is a root email and its value is the list of all emails that belong to the same account
        List<List<String>> result = new ArrayList<>();
        for (List<String> emails : unions.values()) {
            Collections.sort(emails);
            String name = emailToName.get(emails.get(0));
            List<String> account = new ArrayList<>();
            account.add(name);
            account.addAll(emails);
            result.add(account);
        }
        return result;
    }

    public static void main(String[] args) {
        AccountsMerge am = new AccountsMerge();
        List<List<String>> accounts = new ArrayList<>();
        
        // Test case from the example
        accounts.add(Arrays.asList("John", "johnsmith@mail.com", "john_newyork@mail.com"));
        accounts.add(Arrays.asList("John", "johnsmith@mail.com", "john00@mail.com"));
        accounts.add(Arrays.asList("Mary", "mary@mail.com"));
        //accounts.add(Arrays.asList("John", "johnnybravo@mail.com"));

        List<List<String>> mergedAccounts = am.accountsMerge(accounts);
        
        // Print the results
        System.out.println("Merged Accounts:");
        for (List<String> account : mergedAccounts) {
            System.out.println(account);
        }
    }

}
