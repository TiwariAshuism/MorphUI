// Package schema holds SDUI contract version constants and compatibility notes.
package schema

// SDUIV1 is the semantic schema identifier clients may send in Accept or X-Schema-Version.
const SDUIV1 = "sdui.v1"

// UIVersion is the integer bumped only on breaking wire-format changes.
const UIVersion = 1

// MinSupportedUIVersion is the oldest ui_version this BFF still emits for backward-compatible clients.
const MinSupportedUIVersion = 1
