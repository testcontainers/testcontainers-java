Feature: Is it possible to search here?
  Everybody wants to search something

   Scenario Outline: Is search possible here
    Given location is "<location>"
    When I ask is it possible to search here
    Then I should be told "<answer>"

  Examples:
    | location | answer |
    | https://www.google.com/ | YES |
    | https://www.google.com/appsstatus | NOPE |