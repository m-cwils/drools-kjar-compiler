[condition][]there is a person aged {age} or older = $p : Person(age >= {age})
[condition][]there is a person aged less than {age} = $p : Person(age < {age})
[consequence][]mark the person as an adult = $p.setAdult(true);
[consequence][]mark the person as a minor = $p.setAdult(false);
