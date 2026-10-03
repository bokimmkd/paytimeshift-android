/** Server-only QA grant. Clients cannot choose the email or unlock entitlement. */
export function testerEntitlement(user,configuredEmail,now=Date.now()) {
  const allowed=typeof configuredEmail==='string' ? configuredEmail.trim().toLowerCase() : '';
  if(!allowed || !user?.emailVerified || user.disabled || typeof user.email!=='string' || user.email.toLowerCase()!==allowed) return null;
  // The UI lease lasts one day. Every cloud operation checks the allowlist again;
  // removing the server setting revokes test cloud access immediately.
  return {active:true,expiresAt:now+86400000,state:'PTS_TEST_ACCESS',autoRenew:false};
}
