# Privacy Policy for EPrachar

**Last Updated: [Date]**

## Introduction
EPrachar ("we", "our", or "us") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, and protect your information when you use our mobile application.

## Information We Collect

### Phone State Information
- **READ_PHONE_STATE**: We access phone state information to detect when phone calls end
- **READ_CALL_LOG**: We access call log information to monitor incoming and outgoing calls

### SMS Permissions
- **SEND_SMS**: We use this permission to send SMS messages to contacts when a call ends (if enabled by the user)

### Data Collected
- Phone numbers of incoming and outgoing calls
- Call timestamps
- Admin code (user ID) configured in the app
- Message content fetched from our API server

## How We Use Your Information

We use the collected information solely for the following purposes:
1. **Call Detection**: To detect when phone calls end
2. **SMS Sending**: To automatically send SMS messages to callers (only if enabled by the user)
3. **AI WhatsApp Integration**: To trigger webhook notifications to our AI service for WhatsApp messaging (only if enabled by the user)

## Data Storage
- All data is stored locally on your device using Android SharedPreferences
- No call data, phone numbers, or personal information is transmitted to our servers except:
  - When you enable the "AI WhatsApp message" feature, we send call information (phone number, timestamp) to our webhook service to trigger AI-driven WhatsApp messages
  - The webhook service processes this data as per our AI service provider's privacy policy

## Data Sharing
We do NOT sell, trade, or share your personal information with third parties except:
- When you explicitly enable the AI WhatsApp feature, call information is sent to our webhook endpoint for processing

## Your Rights
You have the right to:
- Disable SMS sending at any time through app settings
- Disable AI WhatsApp messaging at any time through app settings
- Uninstall the app to stop all data collection

## Security
We implement appropriate technical measures to protect your information, but no method of transmission over the internet is 100% secure.

## Contact Us
If you have questions about this Privacy Policy, please contact us at:
- Email: [Your Email]
- Website: [Your Website]

## Changes to This Privacy Policy
We may update this Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page.

---

**Note**: Replace [Date], [Your Email], and [Your Website] with your actual information.

