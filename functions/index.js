const {setGlobalOptions} = require("firebase-functions/v2");
const {onDocumentWritten} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

// Optional: cost control (Gen2)
setGlobalOptions({maxInstances: 10});

const TOPIC_ALL = "mess_all";
const CATEGORY_PAYMENT = "PAYMENT";
const ANDROID_CHANNEL_ID = "mess_notifications_channel";

/**
 * Send notification to all users (topic)
 */
async function notifyAll(title, body, data) {
  const message = {
    topic: TOPIC_ALL,
    notification: {title: title, body: body},
    data: data || {},
    android: {
      priority: "high",
      notification: {
        channelId: ANDROID_CHANNEL_ID, // must match Android CHANNEL_ID
      },
    },
  };

  return admin.messaging().send(message);
}

function getNum(obj, key) {
  const v = obj && obj[key];
  if (typeof v === "number") return v;
  if (typeof v === "string") return parseInt(v, 10) || 0;
  return 0;
}

function getStr(obj, key) {
  const v = obj && obj[key];
  return v ? String(v) : "";
}

/**
 * ✅ MEAL NOTIFICATION
 * Fires when meals_daily/{docId} is created/updated.
 * Sends notification ONLY when any meal changes from 0 -> 1.
 */
exports.notifyOnMealUpdate = onDocumentWritten(
    "meals_daily/{docId}",
    async (event) => {
      try {
        const beforeExists = event.data.before.exists;
        const afterExists = event.data.after.exists;
        if (!afterExists) return;

        const before = beforeExists ? event.data.before.data() : null;
        const after = event.data.after.data();

        const memberName = getStr(after, "memberName");
        const date = getStr(after, "date");

        const bBefore = getNum(before, "breakfast");
        const lBefore = getNum(before, "lunch");
        const dBefore = getNum(before, "dinner");

        const bAfter = getNum(after, "breakfast");
        const lAfter = getNum(after, "lunch");
        const dAfter = getNum(after, "dinner");

        const added = [];
        if (bBefore === 0 && bAfter === 1) added.push("Breakfast");
        if (lBefore === 0 && lAfter === 1) added.push("Lunch");
        if (dBefore === 0 && dAfter === 1) added.push("Dinner");

        // If doc created first time, count initial 1's too
        if (!beforeExists) {
          if (bAfter === 1 && !added.includes("Breakfast")) added.push("Breakfast");
          if (lAfter === 1 && !added.includes("Lunch")) added.push("Lunch");
          if (dAfter === 1 && !added.includes("Dinner")) added.push("Dinner");
        }

        if (added.length === 0) return;

        const title = "Meal Updated";
        const who = memberName || "Someone";
        const when = date ? ` (${date})` : "";
        const body = `${who} added: ${added.join(", ")}${when}`;

        await notifyAll(title, body, {
          type: "meal",
          memberName: memberName,
          date: date,
          added: added.join(","),
        });
      } catch (e) {
        console.error("notifyOnMealUpdate error:", e);
      }
    }
);

/**
 * ✅ PAYMENT NOTIFICATION
 * Fires when expenses/{expenseId} is created/updated.
 * Sends notification ONLY when category == PAYMENT and it is NEW (or changed to PAYMENT).
 */
exports.notifyOnPayment = onDocumentWritten(
    "expenses/{expenseId}",
    async (event) => {
      try {
        const beforeExists = event.data.before.exists;
        const afterExists = event.data.after.exists;
        if (!afterExists) return;

        const before = beforeExists ? event.data.before.data() : null;
        const after = event.data.after.data();

        const catAfter = getStr(after, "category").toUpperCase();
        if (catAfter !== CATEGORY_PAYMENT) return;

        const catBefore = getStr(before, "category").toUpperCase();
        if (beforeExists && catBefore === CATEGORY_PAYMENT) return;

        const paidBy = getStr(after, "paidBy");
        const amount = after && after.amount != null ? String(after.amount) : "0";
        const date = getStr(after, "date");
        const titleNote = getStr(after, "title");

        const title = "Payment Received";
        const who = paidBy || "Someone";
        const note = titleNote ? ` - ${titleNote}` : "";
        const when = date ? ` (${date})` : "";
        const body = `${who} paid ${amount}৳${note}${when}`;

        await notifyAll(title, body, {
          type: "payment",
          paidBy: paidBy,
          amount: amount,
          date: date,
        });
      } catch (e) {
        console.error("notifyOnPayment error:", e);
      }
    }
);
