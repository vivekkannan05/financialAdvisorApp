# Canonical profile model

The production profile should support these sections.

## Metadata
- profile_id
- profile_version
- created_at
- updated_at
- default_currency
- jurisdiction_scope

## Household
- adults
- children
- other dependants
- ages / dates of birth where supplied
- support commitments

## Income
Each source should include amount, frequency, owner, stability, taxable/non-taxable notes, as-of date, and source/provenance.

## Expenses
Separate essential, discretionary, support obligations, insurance premiums, debt service, and planned investments.

## Assets
Each asset should include type, subtype, owner, value, liquidity, purpose/goal tag, cost basis if relevant, tax wrapper, as-of date, source, and whether it is protected/earmarked.

## Liabilities and commitments
Include outstanding balance, rate, EMI, remaining term, security, future disbursement schedule where relevant, and known future self-funded commitments.

## Protection
Term insurance, health insurance, property/vehicle cover where relevant, beneficiaries, coverage, premium, expiry/renewal, and gaps.

## Goals
Each goal should include beneficiary, category, present cost/target, target date, inflation assumption, current corpus, linked assets, recurring contribution, priority/constraint notes, and funding status.

## Risk
Store observations rather than one immutable label:
- behavioural responses;
- tolerance score/range;
- capacity score/range;
- requirement by goal;
- current household risk constraints;
- review date.

## Tax and residency
Capture only what is needed for planning: relevant tax residency, regime, countries, FY/AY facts, and specialist conclusions. Avoid storing unnecessary sensitive identifiers.

## Provenance and freshness
Every material field should support:
- value;
- as_of_date;
- source: user | statement | connector | calculated | specialist;
- confidence;
- freshness status.

Do not store passwords, OTPs, full card details, Aadhaar numbers, PAN numbers, or other credentials/secrets as part of the planning profile.
