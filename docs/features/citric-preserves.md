# Citric acid as a preservative

Status: paused work in progress on the contributor's fork. Data not regenerated, no checks or tests run, not proposed for merging yet.

TODO item: "…citrus/citric acid to preservation, sour candy and drinks…". This branch builds on `claude/sour-candy`, which adds citric acid.

## Question for the owner

**Should food spoiling and preservation be universal, only for a few things, or not in the mod at all?**

Today only jars of preserves (jams, fruit butters, jellies, pickles and relish) can spoil. An unsealed jar goes off three days after it was cooked. A jar sealed in the Canning Kettle keeps until it is opened, and then has three days. Every other food keeps for ever, as in vanilla.

The options:

1. **Universal.** Most foods spoil over time, and preservation (canning, salting, smoking, citric acid, cold storage and so on) matters for every food. This is a large change that touches every food item, chest storage and the companions. It needs its own design pass for timings, which foods are exempt, and how stored or carried food is tracked.
2. **Just these few things** (the current state). Only preserve jars spoil. A preservative like citric acid gives an unsealed or opened jar longer to keep.
3. **Not at all.** Remove jar spoiling, so preserves keep for ever. The Canning Kettle would then need another purpose, or would become decorative. Existing jars stay valid.

This branch assumes option 2. If you choose 1 or 3, it should be redone or dropped.

## What this branch does so far

Clicking citric acid (item tag `jugcraft:preservatives`) onto an unspoiled jar in an inventory stirs it in: one citric acid a jar, recorded as the `jugcraft:preserved` component. The jar then keeps twice as long (six days) when unsealed or once opened, and its tooltip says so. The factor of two is for review.
