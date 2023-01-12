export const matches = (event, keysToMatch) => {
  for (let i = 0; i < keysToMatch.length; i++) {
    if (match(event, keysToMatch[i])) {
      return true;
    }
  }
  return false;
};

export const match = (eventOrCode, { key, which, keyCode } = {}) => {
  if (typeof eventOrCode === 'string') {
    return eventOrCode === key;
  }

  if (typeof eventOrCode === 'number') {
    return eventOrCode === which || eventOrCode === keyCode;
  }

  if (eventOrCode.key && Array.isArray(key)) {
    return key.indexOf(eventOrCode.key) !== -1;
  }

  return (
    eventOrCode.key === key ||
    eventOrCode.which === which ||
    eventOrCode.keyCode === keyCode
  );
};

export const getCharacterFor = (eventOrCode) => {
  if (typeof eventOrCode === 'number') {
    return String.fromCharCode(eventOrCode);
  }

  return (
    eventOrCode.key ||
    String.fromCharCode(eventOrCode.which || eventOrCode.keyCode)
  );
};
