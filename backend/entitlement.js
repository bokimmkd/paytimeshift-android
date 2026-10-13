/** Only Google Play server evidence can grant access. Cancellation keeps paid access until expiry. */
export function subscriptionEntitlement(subscription,now=Date.now()) {
  const line=subscription.lineItems?.find(l=>l.productId==='pts_premium' && l.offerDetails?.basePlanId==='annual');
  if(!line) throw Error('Wrong product or base plan');
  const expiry=Date.parse(line.expiryTime ?? '');
  return {active:['SUBSCRIPTION_STATE_ACTIVE','SUBSCRIPTION_STATE_IN_GRACE_PERIOD','SUBSCRIPTION_STATE_CANCELED'].includes(subscription.subscriptionState) && Number.isFinite(expiry) && expiry>now,
    expiresAt:Number.isFinite(expiry)?expiry:0,state:subscription.subscriptionState ?? 'UNKNOWN',autoRenew:line.autoRenewingPlan?.autoRenewEnabled===true};
}
