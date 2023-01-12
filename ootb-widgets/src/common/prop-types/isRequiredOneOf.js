const isRequiredOneOf = (propTypes) => {
  const names = Object.keys(propTypes);
  const checker = (propType) => (props, propName, componentName, ...rest) => {
    return propType(props, propName, componentName, ...rest);
  };
  return names.reduce(
    (o, name) => ({
      ...o,
      [name]: checker(propTypes[name]),
    }),
    {}
  );
};

export default isRequiredOneOf;
