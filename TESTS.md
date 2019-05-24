This is a quick and dirty test plan.

Tests should be performed as such...

- Perform [setup steps](#initial-setup-steps) to prep for the first test **once**.*
- Perform [normal steps](#test-scenario-steps) while using [test case variants](#test-case-variants)

\* setup steps only need to be performed once (as the [test scenario](#test-scenario-steps) sets up for next test case), but **should** be used to get to a safe starting state for a test case when in doubt 

# Initial Setup Steps

We want to start every test with a state where the consumer project already has been compiled since we are testing changes that happen when iterating over the lib.

1.  _In `kotlin-mp-lib`_, run `publishToMavenLocal`
2.  Run the run configuration: `Run JVM`

# Test Scenario Steps

1.  _In `kotlin-mp-lib`_, run `publishToMavenLocal`
perform a change using [the section of variants below](#test-case-variants)
2.  Run `publishToMavenLocal` via double-clicking task in the Gradle Tool window.
3.  _In `<this repo>`_, refresh the linked Gradle projects via the Gradle Tool window.
4.  Run the run configuration: `Run JVM`
_to get ready for the next test, fix any existing errors and re-run_

# Test Case Variants
## Without Composite Configuration
### Multiplatform
Test the effects of changes to a common multiplatform class (expects) that consumers use
#### Public
- [ ] TCID-1 - add parameter to public multiplatform method  
<!--  _=> **RESULT**_-->
- [ ] TCID-2 - remove parameter from public multiplatform method  
<!--  _=> **RESULT**_-->
- [ ] TCID-3 - change body of public multiplatform method without changing signature  
<!--  _=> **RESULT**_-->
#### Private
- [ ] TCID-4 - add parameter to private multiplatform method  
<!--  _=> **RESULT**_-->
- [ ] TCID-5 - remove parameter from private multiplatform method  
<!--  _=> **RESULT**_-->
- [ ] TCID-6 - change body of private multiplatform method without changing signature  
<!--  _=> **RESULT**_-->

### Common Only - JVM
Test the effects of changes to a common only lib class that JVM consumers use
#### Public
- [ ] TCID-7 - add parameter to public common method  
<!--  _=> **RESULT**_-->
- [ ] TCID-8 - remove parameter from public common method  
<!--  _=> **RESULT**_-->
- [ ] TCID-9 - change body of public common method without changing signature  
<!--  _=> **RESULT**_-->
#### Private
- [ ] TCID-10 - add parameter to private common method  
<!--  _=> **RESULT**_-->
- [ ] TCID-11 - remove parameter from private common method  
<!--  _=> **RESULT**_-->
- [ ] TCID-12 - change body of private common method without changing signature  
<!--  _=> **RESULT**_-->
