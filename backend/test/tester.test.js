import test from 'node:test';
import assert from 'node:assert/strict';
import {testerEntitlement} from '../tester.js';

const allowed='bokimk.ap@gmail.com';
const user={email:allowed,emailVerified:true,disabled:false};
test('only owner-approved verified account receives server test access',()=>{
  const ent=testerEntitlement(user,allowed,1000);
  assert.equal(ent.active,true);assert.equal(ent.expiresAt,86401000);
  assert.equal(ent.state,'PTS_TEST_ACCESS');assert.equal(ent.autoRenew,false);
  assert.equal(testerEntitlement({...user,email:'BOKIMK.AP@gmail.com'},allowed,1000).active,true);
});
test('signed-out, unverified, disabled and other accounts remain free',()=>{
  for(const denied of [null,{...user,emailVerified:false},{...user,disabled:true},{...user,email:'boki.mk@gmail.com'},{...user,email:'bokimk@gmail.com'},{emailVerified:true}]) {
    assert.equal(testerEntitlement(denied,allowed),null);
  }
});
test('empty or revoked server allowlist grants no Premium',()=>{
  for(const setting of ['', '  ', null, undefined, 'other@example.com']) assert.equal(testerEntitlement(user,setting),null);
});
