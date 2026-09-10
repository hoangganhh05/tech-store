import { afterEach, describe, expect, it, vi } from "vitest";
import { toDateTimeLocalValue } from "../utils/dateTime";

describe("toDateTimeLocalValue", () => {
  afterEach(() => vi.restoreAllMocks());

  it("converts a UTC timestamp into the browser's local datetime value", () => {
    vi.spyOn(Date.prototype, "getTimezoneOffset").mockReturnValue(-420);

    expect(toDateTimeLocalValue("2026-09-01T00:00:00Z")).toBe("2026-09-01T07:00");
  });

  it("returns an empty value for missing or invalid timestamps", () => {
    expect(toDateTimeLocalValue(null)).toBe("");
    expect(toDateTimeLocalValue("not-a-date")).toBe("");
  });
});
